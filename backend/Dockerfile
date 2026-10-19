# Laundry HUB backend — multi-stage image (Render, Railway or any Docker host).
#   docker build -t laundryhub-backend backend
#   docker run -p 8080:8080 --env-file backend/.env laundryhub-backend
# Configuration comes only from environment variables (see docs/DEPLOYMENT.md); nothing secret is baked in.

# ---------- 1. Build: compile and package the Spring Boot jar ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
# Dependencies first, so they stay cached while only the code changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run in CI / locally (.\mvnw.cmd test); the image build only packages.
RUN mvn -B -q -DskipTests package \
    && cp target/laundryhub-backend-*.jar /src/app.jar

# ---------- 2. Run: small JRE image, non-root user ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S laundryhub && adduser -S laundryhub -G laundryhub \
    && apk add --no-cache curl
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER laundryhub

# Hosts such as Render set PORT; application.yml reads ${PORT:8080}. The sample orders are not loaded
# in production unless FLYWAY_LOCATIONS is overridden.
ENV PORT=8080 \
    FLYWAY_LOCATIONS=classpath:db/migration \
    SHOP_TIME_ZONE=Asia/Manila \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

# Spring Boot Actuator: "UP" only when the app and the database answer.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fs "http://localhost:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
