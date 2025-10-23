from itertools import chain, combinations
from collections import defaultdict
import time
import sys

specificCount = defaultdict(int)
totalCount = defaultdict(int)

start_time = time.time()

def powerset(iterable):
    s = list(iterable)
    return list(chain.from_iterable(combinations(s, r) for r in range(1, len(s)+1)))

cooccurring_edge_file = open(sys.argv[1]+'cooccurring_edges_new')
training_data_file = open(sys.argv[1]+'trainingDataFreebase_3_50_700-NOconcat-newProp_new', 'w')

###for training data without types
# for line in cooccurring_edge_file:
#     line = line.rstrip('\n').split(',')
#     edges = sorted([int(e) for e in line])
#     if 0 in edges:
#         edges.remove(0)
#     edges = [str(e) for e in edges]
#     specificCount[','.join(edges)] += 1
#     subset_edges = powerset(edges)
#     for s in subset_edges:
#         totalCount[','.join(s)] += 1
#
# for k in specificCount:
#     training_data_file.write(k+','+str(totalCount[k])+','+str(specificCount[k])+'\n')


###for training data with types
for line in cooccurring_edge_file:
    line = line.rstrip('\n').split(',')
    edges = sorted([int(e) for e in line])
    if 0 in edges:
        edges.remove(0)
    edges = [str(e) for e in edges]
    training_data_file.write(','.join(edges)+',1,1'+'\n')

print("Completed in %f hrs !!!" % ((time.time() - start_time)/3600))
