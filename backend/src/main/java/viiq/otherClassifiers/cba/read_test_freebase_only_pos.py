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
candidates = [28160, 28161, 28163, 27140, 28165, 27861, 28173, 27150, 30223, 30224, 30225, 30226, 27669, 28182, 30232, 28187, 28702, 28191, 23084, 23088, 26975, 27403, 28854, 23110, 20388, 28747, 30285, 27726, 30221, 28600, 27732, 30222, 27577, 28760, 30187, 26383, 26209, 28770, 28603, 26219, 20082, 28796, 28181, 30336, 20331, 30085, 30061, 28141, 27801, 27291, 27292, 26992, 26793, 27820, 28845, 28847, 27830, 27832, 28859, 27840, 27114, 30155, 20428, 27345, 27609, 20181, 27862, 27863, 27864, 28377, 26330, 26334, 26335, 26336, 26849, 27364, 26854, 30161, 27885, 27887, 27376, 28885, 27392, 28934, 27399, 28375, 28941, 27406, 27407, 26384, 26385, 27925, 27865, 23327, 28124, 27437, 30000, 30001, 30002, 30003, 30004, 30005, 28470, 27447, 27449, 30011, 30012, 30013, 30014, 30015, 30019, 30020, 30006, 30022, 30023, 30024, 30025, 30007, 30030, 30032, 30034, 30035, 30036, 30037, 30038, 30039, 30041, 30042, 30043, 30044, 30045, 30046, 30047, 30051, 30052, 30057, 26987, 30060, 27501, 27502, 27870, 30065, 30066, 30067, 30069, 30071, 30072, 30073, 30076, 30077, 30078, 30080, 30083, 30084, 26501, 30086, 30087, 26505, 30090, 27373, 20368, 30100, 20374, 30103, 30106, 30109, 30110, 30021, 30113, 30115, 30116, 30118, 30119, 30120, 27561, 27562, 27563, 28590, 30127, 27446, 27573, 30208, 27575, 30136, 20409, 27579, 30143, 30145, 30111, 30027, 20424, 28619, 30156, 28113, 20387, 30165, 30167, 28121, 27611, 30172, 27613, 27616, 26598, 30185, 30186, 27559, 30188, 27823, 30190, 27560, 28152, 30201, 28157, 28158, 27647]

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

        print features, class_label
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
