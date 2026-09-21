FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

COPY src ./src

RUN mvn clean package -DskipTests -B

RUN echo "===== PACKAGED EUREKA CONFIG =====" && \
    jar xf target/*.jar BOOT-INF/classes/application.yml && \
    cat BOOT-INF/classes/application.yml

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]