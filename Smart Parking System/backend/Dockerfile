# =============================================================================
# Smart Parking System - Backend Dockerfile
# Multi-stage build: Maven build → JRE 17 slim runtime
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

ARG MAVEN_PROFILE=prod

WORKDIR /build

# Cache dependencies first (Docker layer caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build
COPY src ./src
RUN mvn clean package -P${MAVEN_PROFILE} -DskipTests -B -q \
    && mv target/*.jar app.jar

# ---------------------------------------------------------------------------
# Stage 2: Runtime with slim JRE
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata

# Copy the built JAR from builder stage
COPY --from=builder /build/app.jar /app/app.jar

# Create directory for certificates
RUN mkdir -p /app/certs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# JVM configuration via environment variable
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entry point with proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
