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

# NFS 는 사용자 이름이 아닌 숫자 UID/GID 로 권한을 판단하므로, 원본 저장소 소유자와 맞출 수 있게 값을 고정한다.
# 바꾸면 NFS 원본 디렉터리 소유자(orbit-infrastructure)도 함께 바꿔야 한다
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --system --gid "${APP_GID}" orbit \
 && useradd --system --uid "${APP_UID}" --gid orbit orbit

WORKDIR /app
COPY --from=build /workspace/app.jar app.jar

USER orbit
EXPOSE 8080

# DB_URL, DB_USERNAME, DB_PASSWORD 등 필수 환경 변수는 실행 시 주입
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
