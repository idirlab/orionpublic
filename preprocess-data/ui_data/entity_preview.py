import json
import os
from collections import defaultdict

wiki_to_fb = json.load(open('/mounts/[server_name]/data/wikipedia/wikipedia_to_freebase.json'))
entity_preview = open('entity_preview', 'w')
#preview = defaultdict(str)

for foldername in os.listdir('/mounts/[server_name]/data/wikipedia/wikiextractor/text/'):
    for filename in os.listdir('/mounts/[server_name]/data/wikipedia/wikiextractor/text/'+foldername):
        f = open('/mounts/[server_name]/data/wikipedia/wikiextractor/text/'+foldername+'/'+filename)
        new_article = False
        first_paragraph = False        
        for line in f:
            line = line.rstrip().replace('&amp;', '&')
           
            #print('xxx'+line.rstrip()+'xxx')
            if line.startswith('<doc id='):
                ### this is a new article
                new_article = True
            elif new_article == True:
                title = line
                first_paragraph = True
                new_article = False
            elif first_paragraph == True and line:
                if title in wiki_to_fb and not title.endswith('(disambiguation)') and not line.endswith('may refer to:'):
                    line = line.replace('</a>', '').replace('()', '\b').replace('(, ', '(').replace('(; ', '(')
                    line = line.split('<a href="')
                    line = ''.join([l.split('">')[-1] for l in line])
                    entity_preview.write(wiki_to_fb[title]+'\t'+line+'\n')
                #print line
                first_paragraph = False

#json.dump(preview, open('entity_preview.json', 'w'))


            
