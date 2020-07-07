# Start with a base image containing Java runtime
FROM openjdk:11

# Add Maintainer Info
LABEL maintainer=wizphil@gmail.com

VOLUME /tmp

#COPY target/instantmessenger-0.0.1-SNAPSHOT.jar /app/instantmessenger.jar
#COPY run.sh /app/run.sh
#RUN chmod 755 /app/run.sh

COPY src/main/resources/application.properties /application.properties

# Make port 8080 available to the world outside this container
EXPOSE 8080
EXPOSE 9999

# The application's jar file
ARG JAR_FILE=target/instantmessenger-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} instantmessenger.jar
#ADD run.sh /app/run.sh

# Set environmental variables
ENV JAVA_MAX_HEAP_SPACE 1024m
ENV JAVA_MIN_HEAP_SPACE 1024m
ENV JAVA11_ARGS --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED
ENV GC_ARGS -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45
ENV DEBUG_OPTS ""
ENV HTTP_PROXY "http://XXX.XXX.XXX.XXX:8080/"
ENV HTTPS_PROXY "http://XXX.XXX.XXX.XXX:8080/"
ENV http_proxy "http://XXX.XXX.XXX.XXX:8080/"
ENV https_proxy "http://XXX.XXX.XXX.XXX:8080/"

# Run the jar file
#WORKDIR /app
#ENTRYPOINT /app/run.sh
#ENTRYPOINT ["java","-cp","app:app/lib/*","com.wizphil.instantmessenger.InstantmessengerApplication"]
ENTRYPOINT ["java","-jar","/instantmessenger.jar"]