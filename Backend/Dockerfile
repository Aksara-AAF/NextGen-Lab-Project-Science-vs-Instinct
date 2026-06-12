# Stage 1: Build the application using Gradle
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
# We use global gradle to avoid potential execution permission issues with gradlew on Windows
RUN gradle build -x test --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
