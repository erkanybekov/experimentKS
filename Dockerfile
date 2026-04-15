FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
