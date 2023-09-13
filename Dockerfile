FROM tomcat:9.0-jdk11-temurin

ENV TZ=Europe/Stockholm

RUN mkdir -p /data/ala-bie-hub/config

COPY sbdi/data/config/charts.json /data/ala-bie-hub/config/charts.json
COPY sbdi/data/config/languages.json /data/ala-bie-hub/config/languages.json

COPY build/libs/ala-bie-hub-*-plain.war $CATALINA_HOME/webapps/ROOT.war
