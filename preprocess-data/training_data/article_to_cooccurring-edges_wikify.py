from sentence_splitter import SentenceSplitter, split_text_into_sentences
from collections import defaultdict
import urllib.parse
import requests
import json
import time
import os
import sys
import numpy as np
import nltk
from nltk.tokenize import sent_tokenize

nltk.download('punkt')
splitter = SentenceSplitter(language='en')
width = 3
overlap = 2
wiki_to_fb = json.load(open(sys.argv[1]+'wikipedia_to_freebase_lower.json'))
fb_mapping = json.load(open(sys.argv[1]+'freebase_edge_mapping.json'))
entity_to_types = json.load(open(sys.argv[1]+'entity_to_types.json'))

start_time = time.time()

fout = open(sys.argv[1]+'cooccurring_edges_new', 'w')

def get_links_in_article(links):
    text_to_link = {}
    for l in links:
        text, link = l[2].lower(), l[1].lower()
        text_to_link[text] = link
    return text_to_link

def get_entities_in_sentence(sentence, links):
    entities = set()
    for word in links:
        if word in sentence and word in wiki_to_fb:
            entities.add(wiki_to_fb[word])
        elif links[word] in sentence and links[word] in wiki_to_fb:
            entities.add(wiki_to_fb[links[word]])
    return list(entities)

def get_edges_in_window(entities):
    edges = set()
    for e1 in entities:
        for e2 in entities:
            if (e1+' '+e2 in fb_mapping):
                edges.add(fb_mapping[e1+' '+e2])
    return list(edges)

def get_types_in_window(entities):
    types = set()
    for e in entities:
        if e in entity_to_types:
            types.update(entity_to_types[e])
    return types

for dirname, dirs, files in os.walk(sys.argv[2]):
    cnt = 0
    for filename in files:
        print(filename)
        data = np.load(os.path.join(sys.argv[2], filename), allow_pickle = True)
        for article in data:
            title = article['wiki_title'].lower()
            text = article['article_text_coref_resolved'].lower()
            links = get_links_in_article(article['links_in_article'])
            sentences = sent_tokenize(text)
            num_sentences = len(sentences)

            entities_in_sentences = [get_entities_in_sentence(sentence, links) for sentence in sentences]

            i = 0
            prev_session = ''
            while i < num_sentences:
                entities_in_window = entities_in_sentences[i: min(i + width, num_sentences)]
                entities_in_window = [entity for sentence in entities_in_window for entity in sentence]
                if title in wiki_to_fb:
                    entities_in_window.append(wiki_to_fb[title])
                entities_in_window = list(set(entities_in_window))
                edges = get_edges_in_window(entities_in_window)
                types = get_types_in_window(entities_in_window)
                session = ','.join(edges) + ',' + ','.join(types)
                if session and session != prev_session and len(edges) > 1:
                    fout.write(session+'\n')
                prev_session = session
                i += (width - overlap)
            cnt += 1
            if cnt%1000 == 0:
                print('completed '+str(cnt)+'th article')
        print("Completed "+filename)
print("Completed in %f hrs !!!" % ((time.time() - start_time)/3600))
