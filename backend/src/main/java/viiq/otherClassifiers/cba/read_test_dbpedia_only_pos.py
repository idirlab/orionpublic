import sys
import urllib
import urllib2
import requests
import time
from read_data import * 
from read_data import find_matching_rules, predict_class, _rank_candidates
from read_data import antecedent, consequent, confidence

#time.sleep(20)


url = "http://127.0.0.1:5000/rank_candidates"
candidates = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 39, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 573, 574, 64, 65, 66, 67, 68, 70, 71, 72, 73, 74, 75, 76, 77, 79, 80, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 100, 102, 103, 104, 105, 106, 107, 108, 110, 112, 113, 118, 119, 121, 126, 127, 128, 130, 131, 132, 134, 135, 136, 137, 138, 145, 146, 147, 148, 149, 151, 152, 153, 154, 156, 157, 158, 159, 160, 161, 163, 164, 165, 166, 168, 169, 170, 171, 173, 175, 176, 177, 178, 179, 180, 181, 184, 187, 194, 195, 201, 202, 204, 205, 206, 210, 211, 216, 220, 221, 222, 223, 224, 226, 570, 230, 233, 234, 240, 241, 242, 254, 255, 256, 257, 259, 260, 261, 263, 264, 265, 266, 267, 268, 273, 279, 280, 281, 282, 283, 286, 288, 289, 290, 291, 292, 294, 296, 299, 306, 310, 313, 314, 316, 318, 320, 322, 325, 327, 330, 567, 568, 569, 346, 349, 350, 571, 358, 360, 572, 362, 363, 364, 367, 368, 371, 576, 388, 393, 417, 419, 355, 436, 450, 331, 459, 566, 361, 478, 481, 490, 491]

def get_result(url, features, candidates):
    data = {}
    data['features'] = features
    data['candidates'] = candidates
    
    res = requests.post(url, data)
    json_res = res.json()

    pred_val = json_res['result'][0]
    return pred_val

def get_result_new(features, candidates):
    required_data = set(features)
    matches = find_matching_rules(required_data, antecedent)
    total_features = 0
    prediction_ranked = predict_class(required_data, matches, antecedent, consequent, confidence, total_features)

    ranked_candidates = _rank_candidates(prediction_ranked, candidates, required_data)
    #print features, ranked_candidates[:10]
    return ranked_candidates[:10]

num_correct_flask = 0

flask_pred = []
test_Y = []

num_correct_inc = 0
num_lines = 0
num_correct_top5 = 0
num_correct_top10 = 0
input_file = sys.argv[2] 
test_data = open(input_file)
for t_data in test_data:
    t_data = t_data.strip().split(',')[:-2]
    #t_data = [int(t) for t in t_data]
    t_data = [int(t) for t in t_data if int(t) >= 0]

    for class_ind in range(len(t_data)):
        class_label = t_data[class_ind]
        if class_label < 0:
            continue

        temp = []
        flag = False
        for data_ind in range(len(t_data)):
            if class_ind == data_ind:
                continue
            flag = True
            temp.append(t_data[data_ind])

        if not flag:
            continue

        features = temp 
        pred_val = get_result_new(features, candidates)

        num_lines += 1
        if pred_val[0] == class_label:
            num_correct_inc += 1 

        if class_label in pred_val[:5]:
            num_correct_top5 += 1 

        if class_label in pred_val[:10]:
            num_correct_top10 += 1 

        print pred_val[0], class_label

        flask_pred.append(pred_val[0])
        test_Y.append(class_label)
        if num_lines % 30 == 0:
            print num_correct_inc, num_lines
            print num_correct_top5, num_lines
            print num_correct_top10, num_lines
            sys.stdout.flush()

for i in range(len(test_Y)):
    if test_Y[i] == flask_pred[i]:
        num_correct_flask += 1

accuracy_flask = float(num_correct_flask)/len(test_Y)

msg = "\nTest data size is "+str(len(test_Y))
print "The test accuracy_flask is ", accuracy_flask
msg += "\nThe test accuracy_flask is " + str(accuracy_flask)

f = open('accuracy.txt', 'w')
f.write(msg)
f.close()
