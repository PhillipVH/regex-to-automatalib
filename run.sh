export MAVEN_OPTS="-Xms4g -Xmx8g"
mvn compile
mvn exec:java -Dexec.mainClass="Main"
