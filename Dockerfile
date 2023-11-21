# First phase: Build with Maven
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
# Download dependencies separately to make better use of the cache
RUN mvn dependency:go-offline
COPY src /app/src
COPY .git /app/.git
# Compile and create package
RUN mvn clean package

# Zweite Phase: Ausführen mit Java 17 Temurin
FROM eclipse-temurin:17-jdk as runner
WORKDIR /app
# Kopieren des gebauten Artefakts aus der ersten Phase
COPY --from=builder /app/target/*.jar app.jar
# Starten der Java-Anwendung
CMD ["java", "-jar", "app.jar"]
