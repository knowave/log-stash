FROM docker.elastic.co/logstash/logstash:8.11.0

RUN curl -L -o /usr/share/logstash/logstash-core/lib/jars/mysql-connector-j-8.0.33.jar \
    https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

RUN bin/logstash-plugin install logstash-output-jdbc

COPY pipeline/ /usr/share/logstash/pipeline/