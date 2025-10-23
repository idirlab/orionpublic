from flask import Flask
from flask import request, jsonify
import pickle
import operator
import sys
import pickle
from flask import jsonify

output_folder = sys.argv[1]

def read_data():
    f = open(output_folder+"/feature_index.pickle")
    feature_index = pickle.load(f)
    f.close()

    f = open(output_folder+"/index_feature.pickle")
    index_feature = pickle.load(f)
    f.close()

    f = open(output_folder+"/total_features.pickle")
    total_features = pickle.load(f)
    f.close()

    f = open(output_folder+"/class_index.pickle")
    class_index = pickle.load(f)
    f.close()

    f = open(output_folder+"/index_class.pickle")
    index_class = pickle.load(f)
    f.close()

    return feature_index, index_feature, total_features, \
            class_index, index_class

def convert_data(data, feature_index, class_index=None):
    res = []
    for t in data:
        try:
            val = feature_index[t]
            if class_index:
                val = class_index[t]
            res.append(val)
        except KeyError:
            if class_index:
                res.append(t)
            pass

    return res

def convert_data_back(data, index_feature, index_class):
    res = [] 
    for t in data:
        try:
            val = index_class[t]
            res.append(val)
        except KeyError:
            res.append(t)

    return res

def read_rules(base_path):
    _antecedent = [] 
    _consequent = []
    _confidence = []

    f = open(base_path+"_antecedent")
    for t in f:
        t = t.strip()
        t = t.split(",")
        temp = []
        for _t in t:
            try:
                temp.append(int(_t.strip()))
            except ValueError:
                pass
        _antecedent.append(set(temp))
    f.close()

    f = open(base_path+"_consequent")
    for t in f:
        t = t.strip()
        temp = None 
        try:
            temp = int(t.strip())
        except ValueError:
            pass
        _consequent.append(temp)
    f.close()

    f = open(base_path+"_confidence")
    for t in f:
        t = t.strip()
        temp = None 
        try:
            temp = float(t.strip())
        except ValueError:
            pass
        _confidence.append(temp)
    f.close()

    return _antecedent, _consequent, _confidence

def find_matching_rules(required_data, antecedent):
    matches = []

    for i in range(len(antecedent)):
        match = required_data.intersection(antecedent[i])
        #print match
        if match:
            matches.append((i, len(match)))

    return matches

def predict_class(required_data, matches, antecedent, consequent, confidence, total_features):
    classes = {}

    #print "mathches", matches
    for match in matches:
        class_predicted = consequent[match[0]]
        data_antecedent = antecedent[match[0]]

        score = len(data_antecedent.intersection(required_data))/float(len(required_data))
        score = score * confidence[match[0]]/ 100.0

        try:
            classes[class_predicted] += score
        except:
            classes[class_predicted] = score

    classes = sorted(classes.iteritems(), key=lambda x: operator.itemgetter(1), reverse=True)
    #print classes, type(classes)
    return classes

def candidate_present(prediction_ranked, candidate):
    for (cand, score) in prediction_ranked:
        if cand == candidate:
            return (cand, score)

    return (None, 0) 

def _rank_candidates(prediction_ranked, candidates, required_data):
    _ranked_candidates = []
    not_present = []

    for candidate in candidates:
        (cand, score) = candidate_present(prediction_ranked, candidate)
        if score: 
            _ranked_candidates.append((candidate, score))
        else:
            not_present.append((candidate, score))

    for t in not_present:
        _ranked_candidates.append(t)

    ranked_candidates_with_scores = sorted(_ranked_candidates, key=lambda x: x[1], reverse=True)
    #print "ranked_candidates_with_scores", ranked_candidates_with_scores

    ranked_candidates = [t[0] for t in ranked_candidates_with_scores if t[0] not in required_data]
    return ranked_candidates 

#feature_index, index_feature, total_features, class_index, index_class = read_data()
antecedent, consequent, confidence = read_rules(output_folder+"/test1")

app = Flask(__name__)

@app.route("/rank_candidates", methods=["GET", "POST"])
def rank_candidates(features=None, candidates=None):
    features = request.form.getlist('features')
    candidates = request.form.getlist('candidates')

    features = [int(t) for t in features if int(t) >= 0]
    candidates = [int(t) for t in candidates]

    required_data = set(features)
    matches = find_matching_rules(required_data, antecedent)
    total_features = 0
    prediction_ranked = predict_class(required_data, matches, antecedent, consequent, confidence, total_features)
    #print "required_data", required_data
    #print "matches", matches
    #print "prediction_ranked", prediction_ranked
    #print "original candidates", candidates
    #candidates = convert_data(candidates, feature_index, class_index)
    #print "candidates", candidates
    ranked_candidates = _rank_candidates(prediction_ranked, candidates, required_data)
    #print "ranked_candidates", ranked_candidates
    #ranked_candidates = convert_data_back(ranked_candidates, index_feature, index_class)
    #print "ranked_candidates", ranked_candidates
    #return ranked_candidates
    return jsonify(result=ranked_candidates)

@app.route("/")
def hello():
    return "Hello World!"


#features = [-216,-195,-179,-176,-171,-170,-169,-168,-166,-160,-156,-138,-137,-135,-119,-113,-112,-106,-105,-104,-98,-95,-94,-93,-91,-89,-87,-86,-85,-84,-79,-75,-74,-68,-67,-66,-65,-64,-50,-49,-44,-37,-36,-35,-25,-22,-21,-20,-19,-17,-16,-10,-9,-6,-5,-4,-3,-2,-1,18,291,14,102, 18, 107]
#candidates = [1,2,3,567,19,66,4,5,6,7]
#r = rank_candidates(features, candidates)
#print "==============="
#print "candidates", candidates
#rint "ranked_candidates", r 
#
#features = [-216,-195,-179,-176,-171,-170,-169,-168,-166,-160,-156,-138,-137,-135,-119,-113,-112,-106,-105,-104,-98,-95,-94,-93,-91,-89,-87,-86,-85,-84,-79,-75,-74,-68,-67,-66,-65,-64,-50,-49,-44,-37,-36,-35,-25,-22,-21,-20,-19,-17,-16,-10,-9,-6,-5,-4,-3,-2,-1,18,291,14,102, 18, 107]
#candidates = [1,2,3,4,5,6,7,19,66,567]
#r = rank_candidates(features, candidates)
#print "==============="
#print "candidates", candidates
#print "ranked_candidates", r 

if __name__ == "__main__":
    app.run(port=5000)
