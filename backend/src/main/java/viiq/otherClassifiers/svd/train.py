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

file_path = "/Users/rohitbhoopalam/Documents/thesis/data_3000.csv" 

def save_data():
    global feature_count
    global feature_index
    global similar_users 
    global temp
    global V 
    global num_features
    global data_array 

    pickle.dump(feature_count, open("./data/feature_count.p", "wb"))
    pickle.dump(feature_index, open("./data/feature_index.p", "wb"))
    pickle.dump(similar_users, open("./data/similar_users.p", "wb"))
    pickle.dump(temp, open("./data/temp.p", "wb"))
    pickle.dump(V, open("./data/V.p", "wb"))
    pickle.dump(num_features, open("./data/num_features.p", "wb"))
    pickle.dump(data_array, open("./data/data_array.p", "wb"))

def train_data():
    global feature_count
    global temp
    global V 
    global data_array 
    global num_features

    f = open(file_path, 'r')
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
    f = open(file_path, 'r')
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

if __name__ == "__main__":
    train_data()
