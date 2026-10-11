# NOT VERIFIED: I could not run `docker build` here. The mirror URL comes from
# Google's documentation; the rest is standard Maven configuration.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Maven Central is rate-limiting shared build IPs (HTTP 429), which breaks builds on
# Render. Download dependencies from Google's public Maven Central mirror instead.
RUN printf '%s\n' \
      '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
      '  <mirrors>' \
      '    <mirror>' \
      '      <id>google-maven-central</id>' \
      '      <name>GCS Maven Central mirror</name>' \
      '      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>' \
      '      <mirrorOf>central</mirrorOf>' \
      '    </mirror>' \
      '  </mirrors>' \
      '</settings>' > /app/settings.xml

ENV MVN_ARGS="-B -s /app/settings.xml -Dmaven.repo.local=/app/.m2repo"

COPY pom.xml ./
RUN mvn $MVN_ARGS dependency:go-offline || true
COPY src ./src
RUN mvn $MVN_ARGS clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]