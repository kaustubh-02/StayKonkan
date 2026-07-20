# =====================================================================
# StayKonkan Backend — Multi-stage Docker build
# Stage 1 builds the jar with Maven + a JDK; Stage 2 runs it on a minimal
# JRE-only image so the shipped container has no build tooling, source
# code, or Maven cache in it (smaller image, smaller attack surface).
# =====================================================================

# ---- Stage 1: Build -------------------------------------------------
# Uses a Maven+JDK24 base image directly. If you generate the Maven
# Wrapper locally (`mvn -N wrapper:wrapper`) and commit .mvn/ + mvnw,
# switch this stage to eclipse-temurin:24-jdk-jammy and `./mvnw` instead
# — either approach works, the wrapper just pins the exact Maven version.
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /app

# Copy only the POM first so Docker caches the dependency layer and
# doesn't re-download the internet every time application code changes.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Stage 2: Run -----------------------------------------------------
FROM eclipse-temurin:24-jre-jammy AS run
WORKDIR /app

# Run as a non-root user (OWASP / container hardening baseline)
RUN groupadd -r staykonkan && useradd -r -g staykonkan staykonkan
USER staykonkan

COPY --from=build /app/target/staykonkan-backend.jar app.jar

EXPOSE 8080

# Actuator health endpoint is used by Docker/ECS/K8s liveness checks
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
