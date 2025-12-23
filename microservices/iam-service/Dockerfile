# Multi-stage build for IAM Service
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build application (skip tests for faster builds)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    mkdir -p target/dependency && \
    cd target/dependency && \
    jar -xf ../*.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy application from builder
COPY --from=builder --chown=spring:spring /app/target/dependency/BOOT-INF/lib ./lib
COPY --from=builder --chown=spring:spring /app/target/dependency/META-INF ./META-INF
COPY --from=builder --chown=spring:spring /app/target/dependency/BOOT-INF/classes ./

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM tuning for IAM service (optimized for high throughput)
# - Xms1g/Xmx2g: Smaller heap than Gateway (IAM does less caching)
# - G1GC: Low-latency garbage collector
# - MaxGCPauseMillis: Target max GC pause time
# - UseStringDeduplication: Save memory on repeated strings
ENV JAVA_OPTS="-Xms1g -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
