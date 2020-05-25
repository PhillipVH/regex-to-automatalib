export MAVEN_OPTS="-Xms2g -Xmx4g"
mvn compile
mvn exec:java -Dexec.mainClass="Main"
