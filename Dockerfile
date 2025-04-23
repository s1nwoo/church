# Step 1: Gradle 빌드용 이미지
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# 프로젝트 전체 복사
COPY . .

# gradlew 실행권한 부여
RUN chmod +x ./gradlew

# JAR 빌드
RUN ./gradlew bootJar --no-daemon

# Step 2: 실행 이미지
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 앱 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
