FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/blood-donor-api/pom.xml ./pom.xml
COPY backend/blood-donor-api/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/target/*.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]