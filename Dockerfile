FROM openjdk:11
RUN apt-get update && apt-get -y install postgresql-client && apt-get clean
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app.jar"]
