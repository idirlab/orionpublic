import json
from collections import defaultdict



d = json.load(open('entity_preview.json'))
print len(d)
while True:
        print 'Input: '
	s = raw_input()
	print d[s]


