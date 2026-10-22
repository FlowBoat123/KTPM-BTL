---
name: springdoc-openapi
description: >-
  Sử dụng skill này khi cần tích hợp, cấu hình hoặc tài liệu hóa API bằng Swagger UI và OpenAPI 3
  cho ứng dụng Spring Boot 3.5, bao gồm xác thực Bearer JWT và chú thích endpoint.
---

# SpringDoc OpenAPI & Swagger UI Skill

Hướng dẫn tích hợp và đặc tả API bằng **SpringDoc OpenAPI 3** trên Spring Boot 3.5.x.

## 1. Cấu hình Dependency trong `pom.xml`
Thêm starter OpenAPI UI chính thức tương thích Spring Boot 3:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

## 2. Cấu hình Bean `OpenAPI` với bảo mật JWT
Tạo file cấu hình `com.dental.config.OpenApiConfig`:
```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dentalClinicOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Dental Clinic Management API")
                .description("Hệ thống backend đặt lịch khám nha khoa - Đồ án KTPM-BTL 2026")
                .version("1.0.0")
                .contact(new Contact().name("Dental Clinic Team")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .name("BearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

## 3. Cho phép truy cập trong `SecurityConfig`
Cần cấp quyền truy cập công khai cho các đường dẫn Swagger trong `SecurityFilterChain`:
```java
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
).permitAll()
```

## 4. Chú thích các API Controller
* Đặt `@Tag(name = "Tên nhóm", description = "Mô tả nhóm")` trên class Controller.
* Đặt `@Operation(summary = "Mô tả ngắn gọn", description = "Chi tiết nghiệp vụ")` trên từng method.
* Định nghĩa các mã phản hồi với `@ApiResponse(responseCode = "200", description = "Thành công")`.
* Đánh dấu yêu cầu đăng nhập đối với các API cần token:
  ```java
  @SecurityRequirement(name = "BearerAuth")
  ```
