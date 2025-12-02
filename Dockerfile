# ==============================
# 🏗️ Build stage
# ==============================
FROM bellsoft/liberica-openjdk-alpine:21 AS builder

WORKDIR /app

COPY gradlew .
RUN chmod +x gradlew

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

RUN mkdir -p /root/.gradle
RUN ./gradlew build -x test --dry-run

COPY . .

RUN ./gradlew clean build -x test

# ==============================
# 🚀 Run stage
# ==============================
FROM bellsoft/liberica-openjdk-alpine:21

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]