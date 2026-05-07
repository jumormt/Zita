# syntax=docker/dockerfile:1.6
#
# Multi-stage build for Zita.
#
# Stage 1 (builder) compiles the project with Maven and produces the shaded
# uber-jar at /build/target/Zita.jar.
#
# Stage 2 (runtime) is a thin JRE image that bundles the JAR, the default
# ruleset, and the Python batch-analysis scripts.
#
# Build:
#   docker build -t zita .
#
# Run a single sketch (mounting the sketch directory at /work):
#   docker run --rm -v "$PWD/MySketch:/work" zita \
#     --project /work --rules /app/rules.xml --renderer zita
#
# Run the batch pipeline (override the entrypoint to invoke Python):
#   docker run --rm --entrypoint python3 \
#     -v "$PWD/submissions:/work" -v "$PWD/out:/out" \
#     zita /app/scripts/batch_process_zita.py /work /out
#
# Note: processing-java (Processing IDE's CLI) is intentionally NOT installed.
# DoesItBuildRule shells out to it; without it, that rule degrades to a single
# "failed to build" violation per submission. Other rules are unaffected.

# -----------------------------------------------------------------------------
# Stage 1 — builder
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Resolve dependencies in a separate layer so source-only changes don't
# invalidate the dependency cache.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2 — runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

# Python is required only by the batch-analysis scripts, which override the
# entrypoint. Kept minimal: no pip, no extra packages — the scripts use the
# stdlib only.
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*

# Application files.
COPY --from=builder /build/target/Zita.jar             /app/Zita.jar
COPY src/main/resources/rulesets/rules.xml             /app/rules.xml
COPY scripts                                           /app/scripts

# Convenience env vars consumed by users in shell wrappers; the scripts
# themselves still accept paths positionally.
ENV ZITA_JAR=/app/Zita.jar \
    ZITA_RULES=/app/rules.xml

# Mount your sketches / submissions here at runtime.
WORKDIR /workspace

ENTRYPOINT ["java", "-jar", "/app/Zita.jar"]
CMD ["--help"]
