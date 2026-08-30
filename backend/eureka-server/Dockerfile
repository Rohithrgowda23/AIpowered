# NOT VERIFIED - I could not run `docker build` in the environment this was
# written in (no Docker daemon here, and Maven Central isn't in my network
# allowlist, so I also could not confirm the build actually completes).
# Uses the Maven base image directly (mvn) rather than a generated Maven
# Wrapper (mvnw) - hand-writing a correct mvnw script without being able to
# run `mvn wrapper:wrapper` would be guessing at something security-relevant
# (it downloads and executes a Maven distribution), so this uses the
# image-provided `mvn` instead.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
