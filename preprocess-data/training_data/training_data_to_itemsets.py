#itemset creation for association rule mining
import sys
fin = open(sys.argv[1])
fout = open(sys.argv[1]+'_itemset', 'w')
for line in fin:
    line = line.rstrip('\n').split(',')
    fout.write(' '.join(line[:-2])+'\n')


