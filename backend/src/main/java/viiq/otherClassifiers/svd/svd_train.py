import numpy as np
import scipy as sp
from scipy.sparse import csc_matrix 
import sys
import pickle
np.seterr(divide='ignore', invalid='ignore')

feature_index = {}

feature_count = 0

similar_users = []

temp = ""
V = ""
num_features = 0 

data_array = ""

querylog_file_path = sys.argv[1] #"/Users/rohitbhoopalam/Documents/thesis/data_3000.csv" 
output_folder = sys.argv[2] #"../../../../../../../data/output/python-svd/"

def save_data():
    global feature_count
    global feature_index
    global similar_users 
    global temp
    global V 
    global num_features
    global data_array 

    pickle.dump(feature_count, open(output_folder+"feature_count.p", "wb"))
    pickle.dump(feature_index, open(output_folder+"feature_index.p", "wb"))
    pickle.dump(similar_users, open(output_folder+"similar_users.p", "wb"))
    pickle.dump(temp, open(output_folder+"temp.p", "wb"))
    pickle.dump(V, open(output_folder+"V.p", "wb"))
    pickle.dump(num_features, open(output_folder+"num_features.p", "wb"))
    pickle.dump(data_array, open(output_folder+"data_array.p", "wb"))

def train_data():
    global feature_count
    global temp
    global V 
    global data_array 
    global num_features

    f = open(querylog_file_path, 'r')
    for line in f:
        data = line.split(',')[:-2]
        for t in data:
            t = int(t)
            try:
                feature_index[t]
            except KeyError:
                feature_index[t] = feature_count
                feature_count += 1
    f.close()

    data_list = []
    row = []
    col = []

    row_ind = 0
    f = open(querylog_file_path, 'r')
    for line in f:
        data = line.split(',')[:-2]
        for t in data:
            t = int(t)
            data_list.append(True)
            col.append(feature_index[t])
            row.append(row_ind)
        row_ind += 1
            
    f.close()

    #input data into csc sparse matrix
    data_mat = csc_matrix((data_list, (col, row)))

    data_list = None
    row = None
    col = None

    data_array = data_mat.toarray()
    U, s, V = np.linalg.svd(data_array, full_matrices=False)

    print U.shape, s.shape, V.shape

    S = np.zeros((s.size, s.size))
    S[:s.size, :s.size] = np.diag(s)
    V = V.T

    num_features = min(len(s), 100)
    u2 = U[:, :num_features]
    print U.shape, u2.shape
    eig2 = S[:num_features, :num_features]

    temp = np.dot(u2, np.transpose(np.linalg.inv(eig2)))
    print u2.shape, np.transpose(np.linalg.inv(eig2)).shape

    save_data()
    print "Done with saving the required files"

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

    feature_count = pickle.load(open("./data/feature_count.p", "rb"))
    feature_index = pickle.load(open("./data/feature_index.p", "rb"))
    similar_users = pickle.load(open("./data/similar_users.p", "rb"))
    temp = pickle.load(open("./data/temp.p", "rb"))
    V = pickle.load(open("./data/V.p", "rb"))
    num_features = pickle.load(open("./data/num_features.p", "rb"))
    data_array = pickle.load(open("./data/data_array.p", "rb"))

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

    print b.shape
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
            print "something is wrong"
            continue
        
        if feature_present == 1:
            candidate_score += score
    return candidate_score

def _rank_candidates(features=None, candidates=None):
    find_similar_users(features)

    candidate_score = {}
    total_score = 0

    for candidate in candidates:
        score = get_candidate_score(candidate)
        candidate_score[candidate] = score 
        total_score += score 

    ranked_candidates = sorted(candidate_score.iteritems(), key=lambda t: t[1], reverse=True)
    for t in ranked_candidates:
        print t
    res = [t[0] for t in ranked_candidates]
    return res

def rank_candidates(features=None, candidates=None):
    features = request.form.getlist('features')
    candidates = request.form.getlist('candidates')

    features = [int(t) for t in features]
    candidates = [int(t) for t in candidates]

    print "features=", features
    print "candidates=", candidates
    
    res = _rank_candidates(features, candidates)
    print res
    return jsonify(result=res)

if __name__ == "__main__":
    train_data()
