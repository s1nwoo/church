# 1단계: Gradle로 빌드하는 빌더 이미지
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY . .

# ⚠️ gradle → ./gradlew 로 변경 (프로젝트에 맞는 버전 사용)
RUN ./gradlew bootJar

# 2단계: 빌드된 JAR만 실행하는 경량 이미지
FROM eclipse-temurin:17-jdk-alpine

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 앱 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
