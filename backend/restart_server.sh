#!/bin/sh

pkill -f '/bin/sh ./run_server.sh'
pkill -f 'java -d64 -Xmx200G -Dserver.port=8081 -classpath ./target/src-maven-1.0-SNAPSHOT.jar:./src/lib/weka-stable-3.6.6.jar:./src/lib/apache-commons-httpcore.jar:./src/lib/dsiutils-1.0.12.jar:./src/lib/fastutil-5.1.5.jar:./src/lib/jakarta-commons-collections-3.2.1.jar:./src/lib/jakarta-commons-configuration-1.4.jar:./src/lib/jakarta-commons-lang-2.3.jar:./src/lib/jakarta-commons-logging-1.1.jar:./src/lib/jsap-2.1.jar:./src/lib/json-simple-1.1.1.jar:./src/lib/log4j-1.2.15.jar:./src/lib/spmf.jar viiq.SpringServer ../experimentfiles/properties/querySuggestion_entity_desc_only_linux.properties'
sudo kill -9 $(sudo lsof -t -i:8081) #centos command

rm -f nohup.out
export MAVEN_HOME=/mounts/[server_name]/apache-maven-3.2.2
export M2=$MAVEN_HOME/bin
export PATH=$M2:$PATH
mvn install
#/sbin/service httpd restart #centos command
sudo systemctl restart httpd #archlinux command
nohup ./run_server.sh &
