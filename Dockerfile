# First stage, build frontend
FROM node:20-alpine as frontendbuilder
WORKDIR /app/frontend
COPY frontend /app/frontend
# Download dependencies
RUN npm i
# Build static frontend files
RUN npm run generate

# Second stage: Build with Maven
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
# Clean up
RUN mvn clean
# Download dependencies separately to make better use of the cache
RUN mvn dependency:go-offline
COPY src /app/src
COPY settings.sample.json /app/
COPY schema.settings.json /app/
COPY .git /app/.git
# Copy generated frontend files
COPY --from=frontendbuilder /app/frontend/.output/public /app/target/classes/webstatic
# Compile and create package
RUN mvn package

# Third stage: Execution with Java 17 Temurin
FROM eclipse-temurin:17-jdk as runner
WORKDIR /app
# Copying the built artifact from the first stage
COPY --from=builder /app/target/acmeserver.jar app.jar
# Set parameters to start the Java application
CMD ["java", "-jar", "app.jar"]
