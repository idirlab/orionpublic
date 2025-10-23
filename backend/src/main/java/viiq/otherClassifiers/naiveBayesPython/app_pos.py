from flask import Flask
import numpy as np
import scipy as sp
from flask import request, jsonify
import sys
import pickle
from sklearn.externals import joblib
from scipy.sparse import csc_matrix
import os

output_path = sys.argv[1] 

feature_index = joblib.load(os.path.join(output_path, 'feature_index.pkl'))
index_feature = joblib.load(os.path.join(output_path, 'index_feature.pkl'))
data_classes_dict = joblib.load(os.path.join(output_path, 'data_classes_dict.pkl'))
data_classes_dict_reverse = {}

for key in data_classes_dict:
    data_classes_dict_reverse[data_classes_dict[key]] = key

clf = joblib.load(os.path.join(output_path, 'nb.pkl'))

def predict_classes(in_data, candidates_len):
    test_data = []
    row = []
    col = []

    #print in_data
    #print feature_index.keys()[-10:]
    for t in in_data:
        try:
            index = feature_index[t]
        except KeyError:
            continue
        test_data.append(True)
        col.append(index)
        row.append(0)

    try:
        test_data = csc_matrix((test_data, (row, col)), shape=(max(row)+1, len(feature_index)))
        #print "test_data input to classifier ", test_data
        #res = clf.predict(test_data.todense())
        #res = clf.predict_log_proba(test_data.todense())
        res = clf.predict_proba(test_data.todense())
    except ValueError:
        #this case is to handle all unknown features
        #that is no features passed has been part of 
        #training data
        res = [[0 for t in range(candidates_len)]]
    #print "candidate ranking", res
    return res

app = Flask(__name__)

def _rank_candidates(features=None, candidates=None):
    candidate_score_list = predict_classes(features, len(candidates))
    #return [data_classes_dict_reverse[t] for t in candidate_score_list.tolist()]
    #print candidate_score_list

    candidate_score = {}

    for candidate in candidates:
        try:
            f_index = data_classes_dict[candidate]
        except KeyError:
            candidate_score[candidate] = -sys.maxint
            continue

        try:
            score = candidate_score_list[0][f_index]
        except IndexError:
            score = -sys.maxint

        candidate_score[candidate] = score 

    ranked_candidates = sorted(candidate_score.iteritems(), key=lambda t: t[1], reverse=True)

    print candidate_score
    #for t in ranked_candidates:
    #    print t
    res = [t[0] for t in ranked_candidates]
    return res

@app.route("/rank_candidates", methods=["GET", "POST"])
def rank_candidates(features=None, candidates=None):
    features = request.form.getlist('features')
    candidates = request.form.getlist('candidates')


    features = [int(t) for t in features]
    print "features=", features
    features = [int(t) for t in features if int(t) >= 0]
    candidates = [int(t) for t in candidates]

    print "candidates=", candidates
    
    res = _rank_candidates(features, candidates)

    print "final_ranked candidates", res
    sys.stdout.flush()

    return jsonify(result=res)

@app.route("/")
def hello():
    return "Hello World!"

if __name__ == "__main__":
    print "Starting the app"
    f = [-30000, -30129, -30648, -30112, -30043, -30047, -30100, -30137, -30070, -30012, -30001, -30141, -30003, -30048, -30004, -30733, -28182, -28181, -28179, -28176, -28157, -27447, -27442, -27437, -27424, -27407, -27406, -27404, -27403, -27400, -27392, -27390, -27388, -27386, -27376, -27375, -27374, -27373, -27364, -27277, -26384, 27449, 30002]
    c = [27399, 27382, 27446, 27843, 30596, 30275, 30723, 27869]
    #print _rank_candidates(f, c)
    #app.run(threaded=True, port=5000)
    app.run(port=5000)
