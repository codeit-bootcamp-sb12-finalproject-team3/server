FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew :app-batch:bootJar --no-daemon -x test


FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/app-batch/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]