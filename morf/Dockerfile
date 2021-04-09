FROM tomcat:8.5

RUN mkdir -p /opt/morf

COPY test.sh /opt/morf/a.out
COPY test.sh /opt/morf/b.out
COPY sample.result /opt/morf/sample.result
COPY ffile /opt/morf/ffile

RUN chmod +x /opt/morf/a.out && chmod +x /opt/morf/b.out && chmod +x /opt/morf/ffile

COPY morf.properties /usr/local/tomcat/

COPY target/morf-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/morf.war
