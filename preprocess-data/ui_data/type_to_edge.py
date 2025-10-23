import sys
from collections import defaultdict

type_to_edge = defaultdict(list)

with open(sys.argv[1]) as fp:
	for line in fp:
		text = line.split(',')
		type_to_edge[text[0]].append(text[1])

for end_type in type_to_edge:
	line = end_type + ":"
	for edge in type_to_edge[end_type]:
		line += edge + ','
	print(line)
