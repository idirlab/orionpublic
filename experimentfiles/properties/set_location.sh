#!/bin/bash

LOCATION="/mounts/[server_name]/data/orion/"

sed -i "4s~.*~"baseInputDir="$LOCATION"data_all/input/"~" querySuggestion_entity_desc_only_linux.properties 
sed -i "5s~.*~"baseOutputDir="$LOCATION"data_all/output/"~" querySuggestion_entity_desc_only_linux.properties

sed -i "4s~.*~"baseInputDir="$LOCATION"data_all/input/"~" freebase/querySuggestion_linux-rdp.properties freebase/querySuggestion_linux-blr.properties
sed -i "5s~.*~"baseOutputDir="$LOCATION"data_all/output/"~" freebase/querySuggestion_linux-rdp.properties freebase/querySuggestion_linux-blr.properties
