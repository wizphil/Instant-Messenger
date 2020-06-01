# Start with a base image containing Java runtime
FROM openjdk:11

# Add Maintainer Info
LABEL maintainer=wizphil@gmail.com

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
ARG JAR_FILE=target/instantmessenger-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} instantmessenger.jar

# Set environmental variables
ENV HTTP_PROXY "http://XXX.XXX.XXX.XXX:8080/"
ENV HTTPS_PROXY "http://XXX.XXX.XXX.XXX:8080/"
ENV http_proxy "http://XXX.XXX.XXX.XXX:8080/"
ENV https_proxy "http://XXX.XXX.XXX.XXX:8080/"

# Run the jar file
#ENTRYPOINT ["java","-cp","app:app/lib/*","com.wizphil.instantmessenger.InstantmessengerApplication"]
ENTRYPOINT ["java","-jar","/instantmessenger.jar"]