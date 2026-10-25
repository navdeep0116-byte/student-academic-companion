# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Stage 1: Build stage with Eclipse Temurin Java 21 JDK
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper configuration and descriptor first for optimal layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings to LF (avoid CRLF issues from Windows hosts) and set executable permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Resolve dependencies in batch mode
RUN ./mvnw dependency:go-offline -B || true

# Copy project source code
COPY src/ src/

# Build and package the executable Spring Boot JAR (skip tests during container build)
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Production runtime stage with Eclipse Temurin Java 21 JRE
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Create an unprivileged system user and group to run the application securely
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 --ingroup spring spring

# Copy the built Spring Boot fat JAR from the builder stage
COPY --from=builder --chown=spring:spring /app/target/backend-*.jar app.jar

# Configure container defaults
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose default port
EXPOSE 8080

# Switch to the non-root user
USER spring:spring

# Launch the Spring Boot application, respecting the PORT environment variable provided by Render or defaulting to 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
