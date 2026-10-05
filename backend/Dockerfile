# Multi-stage Spring Boot Dockerfile — shared by all backend services
# Uses the official maven image so no apk/network calls are needed at build time.
# Build with: docker build --build-arg SERVICE_NAME=auth-service -t auth-service ./backend

ARG SERVICE_NAME

# ── Stage 1: Build ──────────────────────────────────────────────────────────
# maven:3.9-eclipse-temurin-21 already has JDK + Maven — no apk needed
FROM maven:3.9-eclipse-temurin-21 AS builder
ARG SERVICE_NAME
WORKDIR /build

# Copy parent pom + common module first (better layer caching for deps)
COPY pom.xml ./pom.xml
COPY common ./common

# Copy ALL service directories so Maven can resolve the parent pom's <modules> list
COPY auth-service ./auth-service
COPY org-service ./org-service
COPY ticket-service ./ticket-service
COPY chat-service ./chat-service
COPY kb-service ./kb-service
COPY analytics-service ./analytics-service
COPY notification-service ./notification-service

# Download deps in a separate cached layer (re-runs only when poms change)
RUN mvn -B -f pom.xml dependency:go-offline -q 2>/dev/null || true

# Build only the target service (common is built automatically via -am)
RUN mvn -B -f pom.xml -pl ${SERVICE_NAME} -am -DskipTests package

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# eclipse-temurin:21-jre-jammy is Debian-based (no apk) — slim and stable
FROM eclipse-temurin:21-jre-jammy AS runtime
ARG SERVICE_NAME
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-XX:+UseG1GC", \
  "-jar", "app.jar"]
