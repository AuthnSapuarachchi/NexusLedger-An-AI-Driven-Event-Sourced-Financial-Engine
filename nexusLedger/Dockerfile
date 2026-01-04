# Step 1: Build the JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the JAR using JDK
FROM eclipse-temurin:21-jre
COPY --from=build /target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]