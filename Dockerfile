FROM eclipse-temurin:23-jdk

ENV CATALINA_HOME=/usr/local/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH
ENV TOMCAT_VERSION=10.1.20

RUN mkdir -p $CATALINA_HOME
WORKDIR $CATALINA_HOME

RUN apt-get update && apt-get install -y wget && \
    wget https://archive.apache.org/dist/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    tar -xvf apache-tomcat-${TOMCAT_VERSION}.tar.gz --strip-components=1 && \
    rm apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    rm -rf webapps/*

COPY ./target/stream-0.0.1-SNAPSHOT.war webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]