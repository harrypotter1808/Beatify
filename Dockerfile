# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/myspotify-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/frontend ./frontend
EXPOSE 4567
CMD ["java", "-jar", "app.jar"]
