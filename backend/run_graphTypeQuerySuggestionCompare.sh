#!/bin/sh

java -Xmx30g -classpath ./target/src-maven-1.0-SNAPSHOT.jar:./src/lib/weka-stable-3.6.6.jar:./src/lib/apache-commons-httpcore.jar:./src/lib/dsiutils-1.0.12.jar:./src/lib/fastutil-5.1.5.jar:./src/lib/jakarta-commons-collections-3.2.1.jar:./src/lib/jakarta-commons-configuration-1.4.jar:./src/lib/jakarta-commons-lang-2.3.jar:./src/lib/jakarta-commons-logging-1.1.jar:./src/lib/jsap-2.1.jar:./src/lib/json-simple-1.1.1.jar:./src/lib/log4j-1.2.15.jar viiq.graphQuerySuggestionMain.GraphQueryAlgosComparisonMain ../querySuggestion_linux.properties
