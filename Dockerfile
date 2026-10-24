# ==============================================================================
# Stage 1: Build source code
# ==============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & pom.xml trước để cache dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy mã nguồn và đóng gói JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 2: Runtime image gọn nhẹ
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo non-root user và thư mục chứa DB SQLite
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

COPY --from=builder /app/target/dental-clinic-*.jar app.jar

ENV PORT=8080 \
    SERVER_ADDRESS=0.0.0.0 \
    SPRING_PROFILES_ACTIVE=postgres,demo

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
