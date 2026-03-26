# 1) BUILD STAGE
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


# 2) RUNTIME STAGE (Tomcat 10.1.52)
FROM tomcat:10.1.52-jdk17-temurin

RUN mkdir -p /data/uploads \
    && chmod -R 777 /data/uploads

VOLUME ["/data/uploads"]

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/storage.war /usr/local/tomcat/webapps/

# Expose Tomcat port
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
