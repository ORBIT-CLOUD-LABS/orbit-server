# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 레이어를 캐시하기 위해 빌드 스크립트를 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

COPY src src
# 테스트는 DB가 필요하므로 이미지 빌드에서 제외
RUN ./gradlew --no-daemon bootJar -x test \
 && mv "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /workspace/app.jar

FROM eclipse-temurin:21-jre AS runtime

RUN groupadd --system orbit && useradd --system --gid orbit orbit

WORKDIR /app
COPY --from=build /workspace/app.jar app.jar

USER orbit
EXPOSE 8080

# DB_URL, DB_USERNAME, DB_PASSWORD 등 필수 환경 변수는 실행 시 주입
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
