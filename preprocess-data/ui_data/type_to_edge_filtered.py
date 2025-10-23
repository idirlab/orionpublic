import sys
from collections import defaultdict

type_to_edge = defaultdict(list)
edge_to_type = defaultdict(list)
type_pair_ratio = defaultdict(tuple)

with open(sys.argv[1]) as fp:
	for line in fp:
		#line = fp.readline();
		text = line.split(',')
                edge_to_type[text[1]].append((text[0],int(text[2])))

with open(sys.argv[2]) as fp:
	for line in fp:
		text = line.split(',')
		type_pair_ratio[(text[0], text[1])] = float(text[2])


for edge in edge_to_type:
	max_cnt = -1
	max_type = None
	for end_type in edge_to_type[edge]:
		if end_type[1] > max_cnt:
			max_cnt = end_type[1]
			max_type = end_type[0]
	#if edge == '47185552':
	#	print max_type
	for end_type in edge_to_type[edge]:
		if (end_type[0], max_type) not in type_pair_ratio or (max_type, end_type[0]) not in type_pair_ratio:
                        continue
		if type_pair_ratio[(end_type[0], max_type)] > 0.1 or type_pair_ratio[(max_type, end_type[0])] > 0.1:
			type_to_edge[end_type[0]].append(edge)


		#print (text[0], text[1])

for end_type in type_to_edge:
	line = end_type + ":"
	for edge in type_to_edge[end_type]:
		line += edge + ','
	print(line)
