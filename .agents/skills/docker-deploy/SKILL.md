---
name: docker-deploy
description: >-
  Sử dụng skill này khi cần tạo Dockerfile, .dockerignore, docker-compose.yml và tối ưu hóa
  việc đóng gói container cho ứng dụng Java 17 Spring Boot và SQLite.
---

# Docker Deployment Skill

Hướng dẫn đóng gói container chuẩn Docker cho ứng dụng Dental Clinic Backend.

## 1. Multi-Stage Dockerfile (`Dockerfile`)
Sử dụng Multi-stage build để đảm bảo dung lượng image tối ưu và an toàn:
```dockerfile
# Stage 1: Build source code
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & pom.xml trước để cache dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy mã nguồn và đóng gói JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime image gọn nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo non-root user và thư mục chứa DB
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

COPY --from=builder /app/target/dental-clinic-*.jar app.jar

ENV PORT=8080 \
    SERVER_ADDRESS=0.0.0.0 \
    DB_PATH=/app/data/dental-clinic.db

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

## 2. File `.dockerignore`
Loại bỏ các file thừa để tăng tốc build image:
```text
target/
.git/
.gitignore
.idea/
*.iml
.mvn/wrapper/maven-wrapper.jar.cache
*.db
*.log
docs/
```

## 3. Khởi chạy với `docker-compose.yml`
```yaml
version: '3.8'

services:
  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: dental-clinic-backend
    ports:
      - "8080:8080"
    environment:
      - PORT=8080
      - SERVER_ADDRESS=0.0.0.0
      - DB_PATH=/app/data/dental-clinic.db
      - SPRING_PROFILES_ACTIVE=demo
    volumes:
      - clinic-data:/app/data
    restart: unless-stopped

volumes:
  clinic-data:
```

## 4. Kiểm tra và chạy thử
* Build image: `docker build -t dental-clinic-backend .`
* Chạy bằng compose: `docker compose up -d`
* Xem logs: `docker compose logs -f`
