# How to build:
# cd docker
# ./build.sh
# OR: docker build -f Dockerfile -t webjwtgen ..

# Build stage
FROM maven:3.8-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package

# Run stage
FROM tomcat:9.0-jdk11-openjdk
WORKDIR /usr/local/tomcat

# Remove default webapps to keep it clean (optional)
RUN rm -rf webapps/*

# Copy WAR from build stage to webapps/webjwtgen.war
# This makes the app available at /webjwtgen
COPY --from=build /app/target/webjwtgen.war webapps/webjwtgen.war

# Generate self-signed certificate for Tomcat HTTPS
# This is for the SERVER'S SSL connection (port 8443)
RUN keytool -genkey -alias tomcat -keyalg RSA \
    -keystore conf/ssl.keystore \
    -storepass changeit -keypass changeit \
    -dname "CN=localhost, OU=WebJWT, O=Dev, L=Seoul, ST=Seoul, C=KR" \
    -validity 3650

# Configure server.xml for HTTPS
# We insert the Connector configuration before the closing </Service> tag
# Using port 8443 for HTTPS
RUN sed -i '/<\/Service>/i \
    <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol" \
    maxThreads="150" SSLEnabled="true" scheme="https" secure="true" \
    clientAuth="false" sslProtocol="TLS" \
    keystoreFile="conf/ssl.keystore" keystorePass="changeit" />' conf/server.xml

# Expose HTTP and HTTPS ports
EXPOSE 8080 8443

CMD ["catalina.sh", "run"]
