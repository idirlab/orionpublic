from flask import Flask
import numpy as np
import scipy as sp
from scipy.sparse import csc_matrix 
from flask import request, jsonify
import sys
import pickle

feature_index = {}

feature_count = 0

similar_users = []

temp = ""
V = ""
num_features = 0 

data_array = ""

output_folder = sys.argv[1] #"/work/03629/rohitb/svd/data/"

def join_columns(mat_a, mat_b, axis=1):
    mat_a = np.array([mat_a])
    mat_b = np.array([mat_b])
    return np.concatenate((mat_a, mat_b), axis=axis)

def load_data():
    global feature_count
    global feature_index
    global similar_users 
    global temp
    global V 
    global num_features
    global data_array 

    feature_count = pickle.load(open(output_folder+"feature_count.p", "rb"))
    feature_index = pickle.load(open(output_folder+"feature_index.p", "rb"))
    similar_users = pickle.load(open(output_folder+"similar_users.p", "rb"))
    temp = pickle.load(open(output_folder+"temp.p", "rb"))
    V = pickle.load(open(output_folder+"V.p", "rb"))
    num_features = pickle.load(open(output_folder+"num_features.p", "rb"))
    data_array = pickle.load(open(output_folder+"data_array.p", "rb"))

def find_similar_users(in_data):
    global temp
    global similar_users 
    test_req = [0 for t in range(feature_count)]
    for t in in_data:
        try:
            test_req[feature_index[int(t)]] = 1
        except KeyError:
            pass
        
    b = np.array(test_req)

    #print b.shape
    bEmbed = np.dot(b, temp)

    user_sim, count = {}, 0

    v2 = V[:, :num_features]
    for x in v2:
        t1 = np.array([bEmbed])
        t2 = np.transpose(np.array([x]))
        user_sim[count] = np.dot(t1, t2)
        user_sim[count] /= (np.linalg.norm(x) * np.linalg.norm(bEmbed))
        count += 1

    similar_users = sorted(user_sim.iteritems(), key=lambda t: t[1], reverse=True)
    similar_users = similar_users[:100]
    similar_users = [(t[0], t[1][0][0]) for t in similar_users if t[1][0][0] > 0.1]

def get_candidate_score(candidate):
    global data_array
    global similar_users
    candidate_score = 0
    for user_ind, score  in similar_users:
        try:
            feature_ind = feature_index[candidate]
        except KeyError:
            continue
        try:
            feature_present = data_array[feature_ind][user_ind]
        except IndexError:
            import pdb
            pdb.set_trace()
            #print "something is wrong"
            continue
        
        if feature_present == 1:
            candidate_score += score
    return candidate_score

app = Flask(__name__)

def _rank_candidates(features=None, candidates=None):
    find_similar_users(features)

    candidate_score = {}
    total_score = 0

    for candidate in candidates:
        score = get_candidate_score(candidate)
        candidate_score[candidate] = score 
        total_score += score 

    ranked_candidates = sorted(candidate_score.iteritems(), key=lambda t: t[1], reverse=True)
    #for t in ranked_candidates:
    #    print t
    res = [t[0] for t in ranked_candidates]
    return res

@app.route("/rank_candidates", methods=["POST"])
def rank_candidates(features=None, candidates=None):
    features = request.form.getlist('features')
    candidates = request.form.getlist('candidates')

    features = [int(t) for t in features]
    candidates = [int(t) for t in candidates]

    #print "features=", features
    #print "candidates=", candidates
    
    res = _rank_candidates(features, candidates)
    #print res
    return jsonify(result=res)

@app.route("/")
def hello():
    return "Hello World!"

if __name__ == "__main__":
    print "Loading the model from file"
    load_data()
    print "Done! Now Launching the web service"
    app.run()
