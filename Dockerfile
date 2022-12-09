FROM tomcat:10.0-jdk17-openjdk
WORKDIR /usr/local/tomcat/webapps
COPY target/scc2223-backend-1.0-1.0.war ROOT.war
EXPOSE 8080