FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -pl order -am clean package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /
COPY --from=build /app/order/target/*.jar app.jar
RUN mkdir -p /uploads
ENTRYPOINT ["java", "-jar", "/app.jar"]