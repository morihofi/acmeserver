# First phase: Build with Maven
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
# Download dependencies separately to make better use of the cache
RUN mvn dependency:go-offline
COPY src /app/src
COPY settings.sample.json /app/
COPY .git /app/.git
# Compile and create package
RUN mvn clean package

# Second stage: Execution with Java 17 Temurin
FROM eclipse-temurin:17-jdk as runner
WORKDIR /app
# Copying the built artifact from the first stage
COPY --from=builder /app/target/acmeserver.jar app.jar
# Set parameters to start the Java application
CMD ["java", "-jar", "app.jar"]
