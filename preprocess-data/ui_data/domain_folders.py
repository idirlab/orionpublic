import sys

prev = ''
with open(sys.argv[1]+'freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode-padded') as fp:
	for line in fp:
		text = line.split(',')
		f = open(sys.argv[1]+'freebase_domain_types/'+text[0], "a")
		f.write(text[1]+'\t'+text[2])
