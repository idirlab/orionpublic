#!/bin/bash

set -e

USERNAME=$1
PASSWORD=$2

mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_to_wiki(mid VARCHAR(255), wiki VARCHAR(255))"
mysql -u $USERNAME -p$PASSWORD freebase -e "LOAD DATA LOCAL INFILE '/xxx-nfs/freebase_to_wikipedia/mid2name.tsv' INTO TABLE freebase_to_wiki"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_mid ON freebase_to_wiki(mid)"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT e.id, f.wiki INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_to_wiki' FIELDS TERMINATED BY '\\t' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entities_id_label e JOIN freebase_to_wiki f ON e.mid=f.mid"

python map_wiki_to_fb.py /mounts/[server_name]/data/orion/

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT subject, predicate, object INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_datagraph' FIELDS TERMINATED BY '\\t' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM freebase_datagraph"

python map_fb-edge_to_entities.py /mounts/[server_name]/data/orion/

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT entity INTO OUTFILE '/mounts/[server_name]/data/orion/person_id' FIELDS TERMINATED BY '\\t' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types WHERE type=1365"

python3 article_to_cooccurring-edges_wikify.py /mounts/[server_name]/data/orion/ /mounts/[server_name]/data/wikipedia/wikiextractor/text/
python generate_training_data.py /mounts/[server_name]/data/orion/

#for association rule mining only
python training_data_to_itemset.py /mounts/[server_name]/data/orion/data_all/output/trainingDataFreebase_3_50_700-NOconcat-newProp 
