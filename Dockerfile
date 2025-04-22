# 1단계: Gradle로 빌드하는 빌더 이미지
FROM gradle:8.5-jdk17 AS builder

# 작업 디렉터리 설정
WORKDIR /app

# 소스 전체 복사
COPY . .

# [기능] 애플리케이션 빌드
# [설명] 컨테이너에 이미 설치된 gradle로 bootJar 실행
RUN gradle bootJar --no-daemon

# 2단계: 빌드된 JAR만 실행하는 경량 이미지
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# [기능] 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
