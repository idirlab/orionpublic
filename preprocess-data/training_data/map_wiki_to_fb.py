import json
import sys
from collections import defaultdict

fin = open(sys.argv[1]+'freebase_to_wiki')
unique_mid = set()
dict = defaultdict(str)

for line in fin:
	line = line.split('\t')
	#dict[line[1].rstrip()] = line[0]
	dict[line[1].rstrip().lower()] = line[0].lower()

fout = open(sys.argv[1]+'wikipedia_to_freebase_lower.json', 'w')
json.dump(dict, fout)
