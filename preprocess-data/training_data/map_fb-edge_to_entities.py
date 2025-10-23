from collections import defaultdict
import json
import sys
dict = defaultdict(set)
edge_cnt = defaultdict(int)

f = open(sys.argv[1]+'freebase_datagraph')

#k = 0
for line in f:

    line = line.split()

    if line[0] > line[2]:
        line[0], line[2] = line[2], line[0]

    dict[line[0]+' '+line[2]].add(line[1])
    edge_cnt[line[1]] += 1


entities_to_edge_mapping = defaultdict(str)
for entity_pair in dict:
    max_count = -1
    max_edge = ''
    for edge in dict[entity_pair]:
        if edge_cnt[edge] > max_count:
            max_count =  edge_cnt[edge]
            max_edge = edge
    entities_to_edge_mapping[entity_pair] = max_edge


json.dump(entities_to_edge_mapping, open(sys.argv[1]+'freebase_edge_mapping.json', 'w'))
