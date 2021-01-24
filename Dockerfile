FROM maven:3.6.3-openjdk-8
WORKDIR /regexlibeval
ADD . .
RUN mvn clean install
ENTRYPOINT bash