import numpy as np
import scipy as sp
from sparsesvd import sparsesvd
from scipy.sparse import csc_matrix 
import sys
np.seterr(divide='ignore', invalid='ignore')

def join_columns(mat_a, mat_b, axis=1):
    mat_a = np.array([mat_a])
    mat_b = np.array([mat_b])
    return np.concatenate((mat_a, mat_b), axis=axis)

feature_index = {}
index_feature = []

feature_count = 0
f = open(sys.argv[1], 'r')
for line in f:
    data = line.split(',')[:-2]
    for t in data:
        t = int(t)
        try:
            feature_index[t]
        except KeyError:
            feature_index[t] = feature_count
            index_feature.append(t)
            feature_count += 1
f.close()

data_list = []
row = []
col = []

row_ind = 0
f = open(sys.argv[1], 'r')
for line in f:
    data = line.split(',')[:-2]
    for t in data:
        t = int(t)
        data_list.append(True)
        #print feature_index, t
        col.append(feature_index[t])
        row.append(row_ind)
    row_ind += 1
        
f.close()

a = np.array([[5,5,0,5], [5,0,3,4], [3,4,0,3],[0,0,5,3],\
            [5,4,4,5],\
            [5,4,5,5]\
            ])

data_mat = csc_matrix((data_list, (col, row)))
#print data_mat

data_list = None
row = None
col = None

#U, s, V = sparsesvd(data_mat, 100000) 
#U = np.transpose(U)
#s = np.array(s)
#V = np.array(V)
#data_mat = None

data_array = data_mat.toarray()
U, s, V = np.linalg.svd(data_array, full_matrices=False)

#print np.dot(ut.T, np.dot(np.diag(st), vt))

print U.shape, s.shape, V.shape
#exit()

S = np.zeros((s.size, s.size))
S[:s.size, :s.size] = np.diag(s)
V = V.T

###t1 = np.transpose(np.array([U[:, 0]]))
###t2 = np.transpose(np.array([U[:, 1]]))
###u2 = np.concatenate((t1, t2), axis=1)
#print U.shape, u2.shape

###v2 = join_columns(V[:, 0], V[:, 1], axis=0)
###v2 = np.transpose(v2)

###eig2 = join_columns(S[:2, 0], S[:2, 1], axis=0)
###
test = [-26384,27364,27374,27375,27376,27386,27388,27390,27392,27399,27449,30048,30002,30648]

test_req = [0 for t in range(feature_count)]
for t in test:
    try:
        test_req[feature_index[int(t)]] = 1
    except KeyError:
        pass
    
####b = np.array([5,5,0,0,0,5])
b = np.array(test_req)
###print b.shape
###print np.linalg.inv(eig2)
###print V, V.shape
###print np.linalg.inv(V)

num_features = min(len(s), 100)
#bEmbed = np.dot(b, np.dot(u2, np.transpose(np.linalg.inv(eig2))))
u2 = U[:, :num_features]
print U.shape, u2.shape
eig2 = S[:num_features, :num_features]

temp = np.dot(u2, np.transpose(np.linalg.inv(eig2)))
print u2.shape, np.transpose(np.linalg.inv(eig2)).shape
print b.shape
bEmbed = np.dot(b, temp)

#print bEmbed
user_sim, count = {}, 0

#print v2
v2 = V[:, :num_features]
for x in v2:
    t1 = np.array([bEmbed])
    t2 = np.transpose(np.array([x]))
    user_sim[count] = np.dot(t1, t2) / (np.linalg.norm(x) * np.linalg.norm(bEmbed))
    count += 1

print user_sim
similar_users = sorted(user_sim.iteritems(), key=lambda t: t[1], reverse=True)
similar_users = similar_users[:100]
similar_users = [(t[0], t[1][0][0]) for t in similar_users if t[1][0][0] > 0.1]

def get_candidate_score(candidate):
    global data_array
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

def rank_candidates(candidates):
    candidate_score = {}
    total_score = 0

    for candidate in candidates:
        score = get_candidate_score(candidate)
        print score, "???????"
        candidate_score[candidate] = score 
        total_score += score 

    ranked_candidates = sorted(candidate_score.iteritems(), key=lambda t: t[1], reverse=True)
    for t in ranked_candidates:
        print t, "::::::"
    return [t[0] for t in ranked_candidates]

#print rank_candidates([-26384,27364,27374,27375,27376,27386,27388,27390,27392,27399,27449,30048,30002,30648])
print rank_candidates(test)
