FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
# Integration tests need MariaDB and are run separately in CI/local verification.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests

FROM build AS test
# Seed the runtime test cache with dependencies already resolved during compilation.
# Preserve later test-run downloads in the Compose Maven volume.
RUN --mount=type=cache,target=/root/.m2 mkdir -p /opt/maven-cache && cp -a /root/.m2/. /opt/maven-cache/
ENTRYPOINT ["sh", "-c", "cp -a -n /opt/maven-cache/. /root/.m2/ && exec \"$@\"", "--"]

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/target/tutormatch-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
