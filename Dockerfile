# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/myspotify-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/frontend ./frontend
EXPOSE 4567
CMD ["java", "-jar", "app.jar"]
