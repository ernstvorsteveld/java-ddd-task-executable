# Docker & Compose Rules

Keep all containerization files (`Dockerfile`, `.dockerignore`, `compose.yaml`) at the project root, completely separate from `src/`.

## `Dockerfile`

- **Multi-stage build**:
  - Build stage uses a pinned Java 27 JDK image to build the Spring Boot executable JAR (`./gradlew bootJar --no-daemon`).
  - Runtime stage uses a minimal pinned Java 27 JRE image (never ship the JDK, Gradle cache, or source code in the final image).
- **Layer caching & `.dockerignore`**:
  - Copy Gradle wrapper and build scripts (`gradlew`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`) before `src/` so dependency layers stay cached.
  - Maintain a `.dockerignore` excluding `build/`, `.gradle/`, `.git/`, `.agents/`, `.kiro/`, and IDE files.
- **Non-root user**: Create and switch to a dedicated non-root user (`USER app`) in the runtime stage.
- **Exec-form `ENTRYPOINT` & JVM flags**: Use exec form (`ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]`) so `SIGTERM` reaches the JVM directly for graceful shutdown.
- **Externalized config only**: Never bake secrets or environment-specific settings into the image; pass runtime settings (`SERVER_PORT`, `TASK_MONGODB_URI`, `TASK_MONGODB_DATABASE`) via environment variables.

## `compose.yaml` (Docker Compose)

- **Modern Compose spec**: Use `compose.yaml` with no deprecated top-level `version:` key.
- **Pinned image versions**: Pin every infrastructure image to an explicit version matching Testcontainers (e.g., `mongo:8.0`; never `:latest`).
- **Healthchecks & startup ordering**: Define `healthcheck` on backing services (e.g., `mongosh --eval "db.adminCommand('ping')"` for MongoDB) and configure `depends_on` with `condition: service_healthy`.
- **Named volumes**: Persist local infrastructure data in named volumes (e.g., `mongo-data:/data/db`) so state survives restarts and can be reset cleanly with `docker compose down -v`.
- **Local developer workflow**: Support both `docker compose up --build` (full stack on `localhost:8080`) and `docker compose up mongo` (MongoDB exposed on `localhost:27017` for `mongosh` inspection or `./gradlew bootRun`).
