#!/bin/bash

set -e

USERNAME=$1
PASSWORD=$2

#wget http://commondatastorage.googleapis.com/freebase-public/rdf/freebase-rdf-latest.gz
#gunzip freebase-rdf-latest.gz
./parse_triples.sh /xxx-nfs/freebase/freebase-rdf-latest

mysql -u $USERNAME -p$PASSWORD  -e "CREATE DATABASE freebase"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase(subject VARCHAR(255), predicate VARCHAR(255), object TEXT)"
mysql -u $USERNAME -p$PASSWORD freebase -e "LOAD DATA LOCAL INFILE '/xxx-nfs/freebase/freebase-rdf-latest_formatted' INTO TABLE freebase"
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE freebase SET object=REPLACE(object, '.', '/') WHERE object NOT LIKE '\"%'"

#create tables for object names, object ids, object types, mediator types, reverse types
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE object_names SELECT * FROM freebase WHERE predicate = '/type/object/name'" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE object_types SELECT * FROM freebase WHERE predicate = '/type/object/type'" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE object_ids SELECT * FROM freebase WHERE predicate = '/type/object/id'" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON object_ids(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE mediator_object_types SELECT * FROM freebase WHERE predicate = '/freebase/type_hints/mediator'" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE reverse_properties SELECT * FROM freebase WHERE predicate = '/type/property/reverse_property'" &
wait

#extract domains
mysql -u $USERNAME -p$PASSWORD freebase -e "create table domains select * from object_types where object = '/type/domain'"
mysql -u $USERNAME -p$PASSWORD freebase -e "create index idx_subject ON domains(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "create table domains_id_label select object_ids.subject AS mid, TRIM(BOTH '\"' FROM object_ids.object) AS label FROM domains join object_ids on domains.subject=object_ids.subject"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM domains_id_label WHERE label REGEXP '^/common/|^/key/|^/type/|^/kg/|^/base/|^/freebase/|^/dataworld/|^/topic_server/|^/user/|^/pipeline/|^/en/|^/kp_lw/|^rdf|^owl'"
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE domains_id_label SET label = replace(label, '/', '')"
mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE domains_id_label ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "OPTIMIZE TABLE domains_id_label"

#extract types
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE types SELECT * FROM object_types WHERE object = '/type/type'"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON types(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE types_id_label SELECT object_ids.subject AS mid, object_ids.object AS label FROM types join object_ids on types.subject=object_ids.subject"
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE types_id_label SET label = TRIM(BOTH '\"' FROM label)"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM types_id_label WHERE label REGEXP '^/common/|^/key/|^/type/|^/kg/|^/base/|^/freebase/|^/dataworld/|^/topic_server/|^/user/|^/pipeline/|^/en/|^/kp_lw/|^rdf|^owl'"
mysql -u $USERNAME -p$PASSWORD freebase -e "SET @max_id = (SELECT MAX(id) FROM domains_id_label); SET @sql = CONCAT('ALTER TABLE types_id_label AUTO_INCREMENT = ', @max_id + 1); PREPARE st FROM @sql; EXECUTE st"
mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE types_id_label ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "OPTIMIZE TABLE types_id_label"

#extract properties
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE properties select * from object_types where object = '/type/property'"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON properties(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE properties_id_label select object_ids.subject AS mid, object_ids.object AS label FROM properties JOIN object_ids ON properties.subject=object_ids.subject"
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE properties_id_label SET label = TRIM(BOTH '\"' FROM label)"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM properties_id_label WHERE label REGEXP '^/common/|^/key/|^/type/|^/kg/|^/base/|^/freebase/|^/dataworld/|^/topic_server/|^/user/|^/pipeline/|^/en/|^/kp_lw/|^rdf|^owl'"
mysql -u $USERNAME -p$PASSWORD freebase -e "OPTIMIZE TABLE properties_id_label"

#extract entities
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE entities_id_label SELECT DISTINCT subject AS mid, TRIM(BOTH '\"' FROM TRIM(TRAILING '@en' FROM object)) AS label FROM object_names WHERE subject REGEXP '^/m/|^/g/' AND object LIKE ('%@en')"
mysql -u $USERNAME -p$PASSWORD freebase -e "SET @max_id = (SELECT MAX(id) FROM types_id_label); SET @sql = CONCAT('ALTER TABLE entities_id_label AUTO_INCREMENT = ', @max_id + 1); PREPARE st FROM @sql; EXECUTE st"
mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE entities_id_label ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE entities_id_label_clean SELECT mid, TRIM('\t' FROM TRIM(TRIM('\t' from TRIM(TRIM('\t' from REPLACE(CONVERT(label USING ASCII), '?', '')))))) AS label, id FROM entities_id_label"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM entities_id_label_clean WHERE length(label) = 0 OR length(label) != length(replace(label, '\n', ''))"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM entities_id_label_clean WHERE label NOT REGEXP '[A-Za-z0-9]'"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_id ON entities_id_label_clean(id)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_label ON entities_id_label_clean(label(60))"

#removing triples
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean SELECT * FROM freebase WHERE subject REGEXP '^/m/|^/g/' AND object REGEXP '^/m/|^/g/' AND predicate NOT REGEXP '^/common/|^/key/|^/type/|^/kg/|^/base/|^/freebase/|^/dataworld/|^/topic_server/|^/user/|^/pipeline/|^/kp_lw/|^rdf|^owl'"

#removing reverse edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_predicate ON freebase_clean(predicate)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON reverse_properties(object(126))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean_no_reverse SELECT f.subject, f.predicate, f.object from freebase_clean f LEFT OUTER JOIN reverse_properties r ON f.predicate=r.object WHERE r.object IS NULL"

#create list of mediator type labels
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE mediator_object_types_labels select t.label FROM mediator_object_types m JOIN types_id_label t ON m.subject=t.mid where m.object ='\"true\"'"

#create list of mediator nodes
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON object_types(object(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE object_types_clean SELECT * FROM object_types WHERE object NOT REGEXP '^/common/|^/key/|^/type/|^/kg/|^/base/|^/freebase/|^/dataworld/|^/topic_server/|^/user/|^/pipeline/|^/en/|^/kp_lw/|^rdf|^owl'"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON object_types_clean(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON object_types_clean(object(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON mediator_object_types_labels(label(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON object_names(subject)"
#identifying nodes in datagraph as mediator if at least one type the it belongs to is a mediator type and it does not have any label
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE mediator_entities SELECT DISTINCT t.subject AS entity FROM object_types t JOIN mediator_object_types_labels m ON t.object=m.label WHERE t.subject NOT IN (SELECT subject FROM object_names)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_entity ON mediator_entities(entity)"

#freebase triples with mediator subject
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean_no_reverse_mediator_subject SELECT * FROM freebase_clean_no_reverse f WHERE f.subject IN (SELECT * FROM mediator_entities)" &

#freebase triples with mediator object
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean_no_reverse_mediator_object SELECT * FROM freebase_clean_no_reverse f WHERE f.object IN (SELECT * FROM mediator_entities)" &

wait

#find edges with both mediator-endpoints (these edges will be removed)
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_spo ON freebase_clean_no_reverse_mediator_subject(subject, predicate, object(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_spo ON freebase_clean_no_reverse_mediator_object(subject, predicate, object(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE double_mediator_edges SELECT s.subject, s.predicate, s.object FROM freebase_clean_no_reverse_mediator_subject s JOIN freebase_clean_no_reverse_mediator_object o WHERE s.subject=o.subject AND s.object=o.object AND s.predicate=o.predicate"

#removing triples having mediator nodes
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean_no_reverse_no_mediator SELECT * FROM freebase_clean_no_reverse WHERE subject NOT IN (SELECT * FROM mediator_entities) AND object NOT IN (SELECT * FROM mediator_entities)"

#removing double-mediator edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_spo ON double_mediator_edges(subject, predicate, object(150))"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM freebase_clean_no_reverse_mediator_subject  WHERE EXISTS (SELECT * FROM double_mediator_edges WHERE subject=freebase_clean_no_reverse_mediator_subject.subject AND predicate=freebase_clean_no_reverse_mediator_subject.predicate AND object=freebase_clean_no_reverse_mediator_subject.object)"
mysql -u $USERNAME -p$PASSWORD freebase -e "DELETE FROM freebase_clean_no_reverse_mediator_object  WHERE EXISTS (SELECT * FROM double_mediator_edges WHERE subject=freebase_clean_no_reverse_mediator_object.subject AND predicate=freebase_clean_no_reverse_mediator_object.predicate AND object=freebase_clean_no_reverse_mediator_object.object)"

#create index for concatenating edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON freebase_clean_no_reverse_mediator_subject(subject)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON freebase_clean_no_reverse_mediator_object(object(150))" &
wait

#concatenating mediator-subject edges with mediator-subject edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE concatenate_edge_pairs_1 SELECT f1.object AS subject, CONCAT(f1.predicate, \"-\", f2.predicate) AS predicate, f2.object AS object  FROM freebase_clean_no_reverse_mediator_subject f1 JOIN freebase_clean_no_reverse_mediator_subject f2 ON f1.subject=f2.subject WHERE f1.predicate > f2.predicate" &

#concatenating mediator-object edges with mediator-object edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE concatenate_edge_pairs_2 SELECT f1.subject AS subject, CONCAT(f1.predicate, \"-\", f2.predicate) AS predicate, f2.subject AS object FROM freebase_clean_no_reverse_mediator_object f1 JOIN freebase_clean_no_reverse_mediator_object f2 ON f1.object=f2.object WHERE f1.predicate > f2.predicate" &

#concatenating mediator-subject edges with mediator-object edges
(mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE concatenate_edge_pairs_3_temp SELECT f2.subject AS subject, f2.predicate AS predicate1, f1.predicate AS predicate2, f1.object AS object FROM freebase_clean_no_reverse_mediator_subject f1 JOIN freebase_clean_no_reverse_mediator_object f2 ON f1.subject=f2.object WHERE f1.predicate != f2.predicate" &&
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE concatenate_edge_pairs_3_temp c SET c.subject = (@temp_s:=c.subject), c.predicate1 = (@temp_p:=c.predicate1), c.subject = c.object, c.predicate1 = c.predicate2, c.object = @temp_s, c.predicate2 = @temp_p WHERE c.predicate1 < c.predicate2" &&
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE concatenate_edge_pairs_3 SELECT subject, CONCAT(predicate1, \"-\", predicate2) AS predicate, object FROM concatenate_edge_pairs_3_temp" &&
mysql -u $USERNAME -p$PASSWORD freebase -e "DROP TABLE concatenate_edge_pairs_3_temp") &

wait

#union of all concatenated edges
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE concatenate_edge_pairs LIKE concatenate_edge_pairs_1"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO concatenate_edge_pairs SELECT * FROM concatenate_edge_pairs_1"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO concatenate_edge_pairs SELECT * FROM concatenate_edge_pairs_2"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO concatenate_edge_pairs SELECT * FROM concatenate_edge_pairs_3"


#create datagraph
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_clean_no_reverse_no_mediator_new LIKE freebase_clean_no_reverse_no_mediator"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO freebase_clean_no_reverse_no_mediator_new SELECT * FROM freebase_clean_no_reverse_no_mediator"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO freebase_clean_no_reverse_no_mediator_new SELECT * FROM concatenate_edge_pairs"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_predicate ON freebase_clean_no_reverse_no_mediator_new(predicate)"
mysql -u $USERNAME -p$PASSWORD freebase -e "INSERT INTO properties_id_label(label) SELECT DISTINCT predicate FROM concatenate_edge_pairs"
mysql -u $USERNAME -p$PASSWORD freebase -e "SET @max_id = (SELECT MAX(id) FROM entities_id_label); SET @sql = CONCAT('ALTER TABLE properties_id_label AUTO_INCREMENT = ', @max_id + 1); PREPARE st FROM @sql; EXECUTE st"
mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE properties_id_label ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_mid ON entities_id_label(mid)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_predicate ON properties_id_label(label(150))" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_all ON freebase_clean_no_reverse_no_mediator_new(subject, predicate, object(150))" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_datagraph SELECT DISTINCT e1.id AS subject, p.id AS predicate, e2.id AS object FROM freebase_clean_no_reverse_no_mediator_new f JOIN properties_id_label p JOIN entities_id_label e1 JOIN entities_id_label e2 WHERE (f.subject=e1.mid AND f.object=e2.mid AND f.predicate=p.label)"

#select most specific source/object type for an edgetype
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE distinct_entities_subject SELECT DISTINCT predicate, subject FROM freebase_datagraph" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE distinct_entities_object SELECT DISTINCT predicate, object FROM freebase_datagraph" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE distinct_entities_subject_cnt SELECT predicate, COUNT(*) as count FROM distinct_entities_subject GROUP BY predicate" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE distinct_entities_object_cnt SELECT predicate, COUNT(*) as count FROM distinct_entities_object GROUP BY predicate" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_predicate ON distinct_entities_subject(predicate)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON distinct_entities_subject(subject)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_predicate ON distinct_entities_object(predicate)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_object ON distinct_entities_object(object)" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_label ON types_id_label(label(64))"

mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE entity_types SELECT e.id AS entity, t.id AS type FROM object_types_clean o JOIN entities_id_label e JOIN types_id_label t ON (o.subject=e.mid AND t.label=o.object)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_entity ON entity_types(entity)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE candidate_subject_endtypes SELECT e.predicate, t.type AS end_type, COUNT(*)/c.count AS percentage FROM distinct_entities_subject e JOIN entity_types t JOIN distinct_entities_subject_cnt c ON (e.subject=t.entity AND c.predicate=e.predicate) GROUP BY e.predicate, t.type HAVING percentage >= 0.9 ORDER BY predicate, percentage" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE candidate_object_endtypes SELECT e.predicate, t.type AS end_type, COUNT(*)/c.count AS percentage FROM distinct_entities_object e JOIN entity_types t JOIN distinct_entities_object_cnt c ON (e.object=t.entity AND c.predicate=e.predicate) GROUP BY e.predicate, t.type HAVING percentage >= 0.9 ORDER BY predicate, percentage"
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "SET @num := 0, @type := ''; CREATE TABLE candidate_subject_endtypes_10 SELECT predicate, end_type, percentage FROM (SELECT predicate, end_type, percentage, @num := if(@type = predicate, @num + 1, 1) AS row_number, @type := predicate AS dummy FROM candidate_subject_endtypes GROUP BY predicate, end_type, percentage HAVING row_number <= 10) AS T" &
mysql -u $USERNAME -p$PASSWORD freebase -e "SET @num := 0, @type := ''; CREATE TABLE candidate_object_endtypes_10 SELECT predicate, end_type, percentage FROM (SELECT predicate, end_type, percentage, @num := if(@type = predicate, @num + 1, 1) AS row_number, @type := predicate AS dummy FROM candidate_object_endtypes GROUP BY predicate, end_type, percentage HAVING row_number <= 10) as T" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_all ON candidate_subject_endtypes_10(predicate, end_type)" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_all ON candidate_object_endtypes_10(predicate, end_type)"
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE type_cnt SELECT type, COUNT(*) AS count FROM entity_types GROUP BY type"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_type ON type_cnt(type)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE type_pair SELECT t1.type AS type1, t2.type AS type2, t2.count AS count2 FROM type_cnt t1 JOIN type_cnt t2"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_pair ON type_pair(type1, type2)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE type_pair_cnt SELECT type1, type2, COUNT(*) AS count, COUNT(*) / count2 AS ratio FROM entity_types e1 JOIN entity_types e2 JOIN type_pair ON e1.entity=e2.entity AND e1.type = type1 AND e2.type = type2 GROUP BY e1.type, e2.type, count2"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE ranked_subject_endtypes_10 SELECT c1.predicate, t.type2 AS type, sum(ratio) AS score FROM candidate_subject_endtypes_10 c1 JOIN candidate_subject_endtypes_10 c2 JOIN type_pair_cnt t ON (c1.predicate=c2.predicate AND c1.end_type=t.type1 AND c2.end_type=t.type2) GROUP BY predicate, type ORDER BY predicate, score" &
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE ranked_object_endtypes_10 SELECT c1.predicate, t.type2 AS type, sum(ratio) AS score FROM candidate_object_endtypes_10 c1 JOIN candidate_object_endtypes_10 c2 JOIN type_pair_cnt t ON (c1.predicate=c2.predicate AND c1.end_type=t.type1 AND c2.end_type=t.type2) GROUP BY predicate, type ORDER BY predicate, score"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_endtypes_subject SELECT r1.predicate, r1.type, r1.score FROM ranked_subject_endtypes_10 r1 LEFT JOIN ranked_subject_endtypes_10 r2 ON r1.predicate=r2.predicate AND r1.score < r2.score WHERE r2.score is NULL"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_endtypes_object SELECT r1.predicate, r1.type, r1.score FROM ranked_object_endtypes_10 r1 LEFT JOIN ranked_object_endtypes_10 r2 ON r1.predicate=r2.predicate AND r1.score < r2.score WHERE r2.score is NULL"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE freebase_endtypes SELECT t1.type AS subject, t1.predicate AS predicate, t2.type AS object FROM freebase_endtypes_subject As t1 JOIN freebase_endtypes_object  AS t2 ON t1.predicate=t2.predicate"

###########create UI data for orion

mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE freebase_datagraph ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, subject, predicate, object INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_datagraph_withoutTypesDomains_source-clean_only_entity_intermediate' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_datagraph ORDER BY subject,predicate,object" &
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, object, predicate, subject INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_datagraph_withoutTypesDomains_object-clean_only_entity_intermediate' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_datagraph ORDER BY object,predicate,subject"
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT p1.id AS id1, p2.id AS id2, p.id AS new_id INTO OUTFILE '/mounts/[server_name]/data/orion/freebaseConcatenatedPropertiesMappingFile-clean' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM properties_id_label p JOIN properties_id_label p1 JOIN properties_id_label p2 ON (p1.label=SUBSTRING_INDEX(p.label,'-',1) AND p2.label=SUBSTRING_INDEX(p.label,'-',-1) AND p.mid IS NULL)"
sed -i 's/\t/,/' /mounts/[server_name]/data/orion/freebaseConcatenatedPropertiesMappingFile-clean
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, subject, predicate, object INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_datagraph_sorted_by_predicate_source' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_datagraph ORDER BY predicate,subject,object" &
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, object, predicate, subject INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_datagraph_sorted_by_predicate_target' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_datagraph ORDER BY predicate,object,subject"
wait

#mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT DISTINCT e.type, f.predicate INTO OUTFILE '/mounts/[server_name]/data/orion/subject_types' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM entity_types e JOIN freebase_datagraph f ON e.entity=f.subject" &
#mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT DISTINCT e.type, f.predicate INTO OUTFILE '/mounts/[server_name]/data/orion/object_types' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM entity_types e JOIN freebase_datagraph f ON e.entity=f.object" &
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT e.type, f.predicate, COUNT(*) INTO OUTFILE '/mounts/[server_name]/data/orion/subject_types_cnt' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM entity_types e JOIN freebase_datagraph f ON e.entity=f.subject GROUP BY e.type, f.predicate" &
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT e.type, f.predicate, COUNT(*)  INTO OUTFILE '/mounts/[server_name]/data/orion/object_types_cnt' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM entity_types e JOIN freebase_datagraph f ON e.entity=f.object GROUP BY e.type, f.predicate" &
wait
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT type1, type2, ratio INTO OUTFILE '/mounts/[server_name]/data/orion/type_pair_ratio' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM type_pair_cnt"
# python type_to_edge.py /mounts/[server_name]/data/orion/subject_types_cnt /mounts/[server_name]/data/orion/type_pair_ratio > /mounts/[server_name]/data/orion/freebase_edgetypes_source_idsorted_edges-clean-nounicode
# python type_to_edge.py /mounts/[server_name]/data/orion/object_types_cnt /mounts/[server_name]/data/orion/type_pair_ratio > /mounts/[server_name]/data/orion/freebase_edgetypes_object_idsorted_edges-clean-nounicode
python type_to_edge_filtered.py /mounts/[server_name]/data/orion/subject_types_cnt > /mounts/[server_name]/data/orion/freebase_edgetypes_source_idsorted_edges-clean-nounicode_filtered
python type_to_edge_filtered.py /mounts/[server_name]/data/orion/object_types_cnt > /mounts/[server_name]/data/orion/freebase_edgetypes_object_idsorted_edges-clean-nounicode_filtered

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT * INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetype_relation_id-nointermediateedges-NOconcat-clean-newProp' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_endtypes"


mysql -u $USERNAME -p$PASSWORD freebase -e "SET @max_id = (SELECT MAX(id) FROM properties_id_label); SET @sql = CONCAT('ALTER TABLE mediator_entities AUTO_INCREMENT = ', @max_id + 1); PREPARE st FROM @sql; EXECUTE st"
mysql -u $USERNAME -p$PASSWORD freebase -e "ALTER TABLE mediator_entities ADD id INT AUTO_INCREMENT PRIMARY KEY"
mysql -u $USERNAME -p$PASSWORD freebase -e "UPDATE mediator_entities m JOIN entities_id_label e ON m.entity=e.mid SET m.id=e.id"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id INTO OUTFILE '/mounts/[server_name]/data/orion/intermediateNodes-new' LINES TERMINATED BY '\\n' FROM mediator_entities"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT COUNT(*), type  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetypes_instances_count' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM entity_types GROUP BY type"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT COUNT(*), predicate  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edges_instances_count' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM freebase_datagraph GROUP BY predicate"

#mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, SUBSTRING_INDEX(label,'/',-2)  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_propertiesMappingFile-nointermediateedges-NOconcat-newProp_lang_en' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM properties_id_label"
mysql -u root -pi5i1n4MN freebase -e "CREATE TABLE t (SELECT id, SUBSTRING_INDEX(label,'/', -2) AS newlabel FROM properties_id_label  WHERE label NOT LIKE '%-%') UNION (SELECT id, CONCAT(SUBSTRING_INDEX(SUBSTRING_INDEX(label,'/', 4),'/',-2),SUBSTRING_INDEX(label,'/', -2)) AS newlabel FROM properties_id_label WHERE label LIKE '%-%')"
mysql -u root -pi5i1n4MN freebase -e "SELECT t1.id, REPLACE(t1.newlabel, '-', '--') INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_propertiesMappingFile-nointermediateedges-NOconcat-newProp_lang_en' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM t t1 WHERE (t1.newlabel IN ('person/gender', 'person/place_of_birth', 'person/nationality', 'person/profession', 'deceased_person/place_of_death', 'film_festival/location', 'book_edition/book', 'location/contains') OR NOT EXISTS (SELECT t2.newlabel from t t2 WHERE t2.newlabel LIKE CONCAT('%-', t1.newlabel) OR  t2.newlabel LIKE CONCAT(t1.newlabel, '-%')))"
mysql -u root -pi5i1n4MN freebase -e "DROP TABLE t"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, SUBSTRING_INDEX(label,'/',-1)  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetypes_idsorted_label_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM types_id_label"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, label  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_domain_lang_en' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM domains_id_label ORDER BY label ASC"

sed -i -e '1i-1    Select Domain...\' /mounts/[server_name]/data/orion/freebase_domain_lang_en


mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT SUBSTR(label, 1, 60), id INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_entities_labelsorted_first_id_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entities_id_label_clean ORDER BY label ASC"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, SUBSTR(label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_entities_idsorted_label_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entities_id_label_clean ORDER BY id ASC"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT t.type, t.entity, SUBSTR(e.label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types t JOIN entities_id_label_clean e ON t.entity=e.id ORDER BY t.type ASC, e.label ASC"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT DISTINCT d.id, e.id, SUBSTR(e.label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_domain-idsorted_instances_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types et JOIN types_id_label t JOIN domains_id_label d JOIN entities_id_label_clean e ON (et.type=t.id AND TRIM(LEADING '/' FROM SUBSTRING_INDEX(t.label, '/', 2))=d.label AND e.id=et.entity) ORDER BY d.id ASC, e.label ASC"

mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT REPLACE(SUBSTRING_INDEX(label,'/',-1), '_', ' ') AS type, id  INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetypes_labelsorted_first_id_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM types_id_label ORDER BY type ASC"


mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT d.id, t.id, REPLACE(SUBSTRING_INDEX(t.label,'/',-1), '_', ' ') AS t_label INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_domain-idsorted_edgetypes_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM types_id_label t JOIN domains_id_label d ON (TRIM(LEADING '/' FROM SUBSTRING_INDEX(t.label, '/', 2))=d.label) ORDER BY d.id ASC, t_label ASC"

(mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_type ON entity_types(type)" &&
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT  e.entity, e.type, REPLACE(SUBSTRING_INDEX(t.label,'/',-1), '_', ' ') AS t_label INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_instances-idsorted_edgetypes_lang_en-clean-nounicode' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types e JOIN types_id_label t ON e.type=t.id ORDER BY e.entity ASC, t_label ASC")

###########create entity, type, domain preview for UI
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE descriptions SELECT subject, TRIM(TRAILING '\"@en' FROM TRIM(LEADING '\"' FROM object)) AS object FROM freebase WHERE predicate='/common/topic/description' AND object LIKE '%@en'"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_subject ON descriptions(subject)"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, REPLACE(REPLACE(object, '\\n', ' '), '\\r', ' ') AS description INTO OUTFILE '/mounts/[server_name]/data/orion/entity_preview' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM descriptions JOIN entities_id_label ON subject=mid"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, REPLACE(REPLACE(object, '\\n', ' '), '\\r', ' ') AS description INTO OUTFILE '/mounts/[server_name]/data/orion/type_preview' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM descriptions JOIN types_id_label ON subject=mid"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, REPLACE(REPLACE(object, '\\n', ' '), '\\r', ' ') AS description INTO OUTFILE '/mounts/[server_name]/data/orion/domain_preview' FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' FROM descriptions JOIN domains_id_label ON subject=mid"

###########create edge preview for UI
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE edge_preview SELECT subject, predicate, object FROM freebase_datagraph GROUP BY predicate"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT e1.label AS subject, p.predicate AS predicate, e2.label AS object INTO OUTFILE '/mounts/[server_name]/data/orion/edge_preview' FIELDS TERMINATED BY '@' LINES TERMINATED BY '\\n' FROM edge_preview p JOIN entities_id_label_clean e1 JOIN entities_id_label_clean e2 ON (p.subject=e1.id AND p.object=e2.id)"

##########create entity files containing only entities with desciption
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_id ON entity_descriptions(id)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE TABLE entities_id_label_clean_desc SELECT e.mid, e.label, e.id FROM entities_id_label_clean e WHERE e.id IN (SELECT id FROM entity_descriptions)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_id ON entities_id_label_clean_desc(id)"
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_label ON entities_id_label_clean_desc(label(60))"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT SUBSTR(label, 1, 60), id INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_entities_labelsorted_first_id_lang_en-clean-nounicode_desc' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entities_id_label_clean_desc ORDER BY label ASC"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT id, SUBSTR(label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_entities_idsorted_label_lang_en-clean-nounicode_desc' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entities_id_label_clean_desc ORDER BY id ASC"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT t.type, t.entity, SUBSTR(e.label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_edgetypes-idsorted_instances_first_lang_en-clean-nounicode_desc' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types t JOIN entities_id_label_clean_desc e ON t.entity=e.id ORDER BY t.type ASC, e.label ASC"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT DISTINCT d.id, e.id, SUBSTR(e.label, 1, 60) INTO OUTFILE '/mounts/[server_name]/data/orion/freebase_domain-idsorted_instances_lang_en-clean-nounicode_desc' FIELDS TERMINATED BY ',' ESCAPED BY '' LINES TERMINATED BY '\\n' FROM entity_types et JOIN types_id_label t JOIN domains_id_label d JOIN entities_id_label_clean_desc e ON (et.type=t.id AND TRIM(LEADING '/' FROM SUBSTRING_INDEX(t.label, '/', 2))=d.label AND e.id=et.entity) ORDER BY d.id ASC, e.label ASC"

#create type clique Files
mysql -u $USERNAME -p$PASSWORD freebase -e "create table types_pair_clique select distinct t1.type as type1, t2.type as type2, t3.type as type3 from entity_types t1 join entity_types t2 on (t1.entity=t2.entity and t2.entity=t3.entity) order by t1.type, t2.type;"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT type1, type2 INTO OUTFILE '/mounts/[server_name]/proj/orion_data/data_all/input/freebase/freebase_types_clique' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM types_clique"
mysql -u $USERNAME -p$PASSWORD freebase -e "create table types_pair_clique select distinct t1.type as type1, t2.type as type2, t3.type as type3 from entity_types t1 join entity_types t2 join entity_types t3 on (t1.entity=t2.entity and t2.entity=t3.entity) where t1.type < t2.type order by t1.type, t2.type"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT type1, type2, type3 INTO OUTFILE '/mounts/[server_name]/proj/orion_data/data_all/input/freebase/freebase_types_pair_clique' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM types_pair_clique"

#find entity degree (number of incident edges) in the datagraph
mysql -u root -pi5i1n4MN freebase -e "SELECT *  INTO OUTFILE '/mounts/[server_name]/data/orion/entity_edge_cnt' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM SELECT entity, count(*) as edge_cnt FROM (SELECT subject as entity FROM freebase_datagraph UNION ALL SELECT object as entity FROM freebase_datagraph) t GROUP BY entity"

#types to entities (ordered by label) mapping generation
mysql -u $USERNAME -p$PASSWORD freebase -e "CREATE INDEX idx_mid ON entities_id_label_clean_desc"
mysql -u $USERNAME -p$PASSWORD freebase -e "SELECT t.id AS type, e.id AS entity INTO OUTFILE '/mounts/[server_name]/data/orion/type_to_entities' FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' FROM object_types_clean o JOIN entities_id_label_clean_desc e JOIN types_id_label t ON (o.subject=e.mid AND t.label=o.object) ORDER BY e.label ASC"

javac CreateMultipleFilesForKeywordSearch.java
java CreateMultipleFilesForKeywordSearch /mounts/[server_name]/data/orion/

wait

javac PadAndAlignFile.java
java PadAndAlignFile /mounts/[server_name]/data/orion/

mkdir /mounts/[server_name]/data/orion/freebase_domain_types
python domain_folders.py /mounts/[server_name]/data/orion/
sed -i -e '1i0       Select Type...\' /mounts/[server_name]/data/orion/freebase_domain_types/*

mkdir /mounts/[server_name]/data/orion/sourcePropertyTables /mounts/[server_name]/data/orion/targetPropertyTables
javac CreatePropertyTables.javac
java CreatePropertyTables /mounts/[server_name]/data/orion/

#manual imputation
################################
#in freebase_edgetypes_source_idsorted_edges-clean-nounicode_filtered, manually imputed 47185561 and 47185584 for type 1077
