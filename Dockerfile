# ===================================================================
# Stage 1: Build the Spring Boot application using Maven and Java 17
# ===================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy project definition and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package the executable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ===================================================================
# Stage 2: Lightweight runtime environment with Java 17 JRE
# ===================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/rentroll-*.jar app.jar

# Expose default port
EXPOSE 8085

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]

