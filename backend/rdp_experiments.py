import subprocess
import time
import os

# N = ['0', '30', '50', '60']
# W = ['false', 'true']
# K = ['-1', '5', '10']
# C = ['0', '1', '2']

# start = time.time()

# for n in N:
#     for w in W:
#         for k in K:
#             for c in C:
#                 print('writing file rdp-experiment-results/rdp-experiment-n_'+ n + '-w_'+ w + '-k_'+ k+ '-c_'+ c)
#                 subprocess.check_call(['./run_graphTypeQuerySuggestionCompare-freebase-rdp.sh', n, w, k, c], stdout=open('rdp-experiment-results/rdp-experiment-n_'+ n + '-w_'+ w + '-k_'+ k+ '-c_'+ c,'w'))

# print('total time taken = '+ str(time.time()-start))

subprocess.check_call(["touch", "rdp-experiment-merged"])
path = '/mounts/[server_name]/projects/orion/backend/rdp-experiment-results'
pattern = "SUGGESTION"
for filename in os.listdir(path):
    suffix = filename.replace("rdp-experiment-", "")
    f = path+"/"+filename
    p1=subprocess.Popen(["grep", pattern, f], stdout=subprocess.PIPE)
    fout = open('temp-rdp-iteration-counts-temp', 'wb')
    subprocess.check_call(["cut", "-d",  " ", "-f", "6"], stdin=p1.stdout, stdout=fout)
    subprocess.check_call(["awk", "NR==1{print \""+suffix+"\"}1", "temp-rdp-iteration-counts-temp"], stdout=open('temp-rdp-iteration-counts','w'))
    subprocess.check_call(["paste", "rdp-experiment-merged", "temp-rdp-iteration-counts"], stdout=open('temp-rdp-experiment-results','w'))
    subprocess.check_call(["mv", "temp-rdp-experiment-results", "rdp-experiment-merged"])
    subprocess.check_call(["rm", "-f", "temp-rdp-experiment-results"])
    subprocess.check_call(["rm", "-f", "temp-rdp-iteration-counts"])
    subprocess.check_call(["rm", "-f", "temp-rdp-iteration-counts-temp"])