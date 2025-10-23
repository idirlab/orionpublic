from sentence_splitter import SentenceSplitter, split_text_into_sentences
from collections import defaultdict
import urllib.parse
import requests
import json
import time
import os
import sys

splitter = SentenceSplitter(language='en')
wiki_to_fb = json.load(open(sys.argv[1]+'wikipedia_to_freebase.json'))
fb_mapping = json.load(open(sys.argv[1]+'freebase_edge_mapping.json'))


## loading list of Person type entities
person_entities = set()
with open(sys.argv[1]+'person_id') as fp:
    for line in fp:
        person_entities.add(line.rstrip())

print('done loading others')

start_time = time.time()

fout = open(sys.argv[1]+'cooccurring_edges', 'w')

def wikify(text, dictionary):
    wikilinks = []
    text = text.lower()
    sentences = splitter.split(text=text)
    words = [word.rstrip(',').rstrip('.') for sentence in sentences for word in sentence.split()]
    for key in dictionary:
        value = dictionary[key]
        if key in text:
            wikilinks.append(value)
        elif key != value.lower() and value.lower() in text:
            wikilinks.append(value)
        elif value in wiki_to_fb and wiki_to_fb[value] in person_entities:
            try:
                fn = key.split()[0]
                ln = key.split()[-1]
                if (fn in words or ln in words):
                    wikilinks.append(value)
            except IndexError:
                continue

    return wikilinks

def get_session(links_in_window):

    if links_in_window:
        edges = set()
        for l1 in links_in_window:
            for l2 in links_in_window:
                if (l1+' '+l2 in fb_mapping):
                    edges.add(fb_mapping[l1+' '+l2])
        if len(edges) > 1:
            session = ''
            for e in edges:
                session += (str(e)+',')
            session = session.rstrip(',')
            return session

    return None

def find_cooccurring_edges(title, text, dictionary, w_c, w_r):
    text = text.strip().split('\n')
    for line in text:
        sentences = splitter.split(text=line)
        w_r = min(w_r, len(sentences))
        links_in_window = []

        prev_session = ''

        entities_in_w_r = []
        for i in range(len(sentences) - w_r + 1):
            links_in_window = [title]
            window_of_sentences = ' '.join(sentences[i: i + w_r])

            sub_sentences = window_of_sentences.split('<a href="')
            for j, sub_sentence in enumerate(sub_sentences):
                if j == 0:
                    result = wikify(sub_sentence, dictionary)
                    links_in_window = links_in_window + result
                else:
                    link = urllib.parse.unquote(sub_sentence.split('"')[0])
                    links_in_window.append(link)
                    try:
                        result = wikify(sub_sentence.split('">')[1].split('</a>')[1], dictionary)
                        links_in_window = links_in_window + result
                    except IndexError:
                        continue

            entities = []
            for link in links_in_window:
                if link in wiki_to_fb:
                    entities.append(wiki_to_fb[link])
            entities_in_w_r.append(entities)

        w_c = min(w_c, len(sentences))
        for i in range(len(sentences) - w_c + 1):
            entities_in_w_c = [entity for entity_list in entities_in_w_r[i: i + w_c] for entity in entity_list]
            session = get_session(entities_in_w_c)
            if session is not None and session != prev_session:
                fout.write(session+'\n')
                prev_session = session


for foldername in os.listdir(sys.argv[2]):
    for filename in os.listdir(sys.argv[2]+foldername):
        f = open(sys.argv[2]+foldername+'/'+filename)
        word_to_link = defaultdict(str)
        new_article = False
        buff = ''
        title = ''

        for line in f:
            if line.startswith('<doc id='):
                new_article = True
            elif line.startswith('</doc>'):
                find_cooccurring_edges(title, buff, word_to_link, 3, 2)

                word_to_link.clear()
                buff = ''
            else:
                if new_article == True:
                    title = line.rstrip()
                    new_article = False
                    word_to_link = defaultdict(str)
                elif not line.isspace():
                    buff += line

                    s = line.split('<a href="')
                    s.pop(0)

                    for ss in s:
                        try:
                            link = urllib.parse.unquote(ss.split('">')[0])
                            words = ss.split('">')[1].split('</a>')[0].lower()
                            word_to_link[words] = link
                        except IndexError:
                            continue

        print('done '+foldername+'/'+filename)
        f.close()
print("Completed in %f hrs !!!" % ((time.time() - start_time)/3600))
