import json
import sys
from collections import defaultdict

fin = open(sys.argv[1]+'freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode_desc')
dict = defaultdict(list)

for line in fin:
    line = line.split(',')
    if line[1] in dict:
        dict[line[1]].append(line[0])
    else:
        dict[line[1]] = [line[0]]

fout = open(sys.argv[1]+'entity_to_types.json', 'w')
json.dump(dict, fout)
