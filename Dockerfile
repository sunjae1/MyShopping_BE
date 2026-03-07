# 빌드 스테이지
FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon -x test

# 실행 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 업로드 폴더 기본 경로
RUN mkdir -p /app/UploadFolder

# 포트 개방
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
