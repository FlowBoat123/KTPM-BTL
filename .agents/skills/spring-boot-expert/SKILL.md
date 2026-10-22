---
name: spring-boot-expert
description: >-
  Sử dụng skill này khi cần viết, sửa, refactor hoặc kiểm thử mã nguồn Java Spring Boot 3.5,
  bao gồm kiến trúc phân tầng (Controller -> Service -> Repository), DTO validation,
  Spring Security JWT và Hibernate/JPA.
---

# Spring Boot Expert Skill

Bộ quy chuẩn phát triển cho dự án Dental Clinic Backend sử dụng **Java 17+ và Spring Boot 3.5.x**.

## 1. Kiến trúc phân tầng (Layered Architecture)
Tuân thủ nghiêm ngặt quy tắc độc lập giữa các tầng:
* **Tầng Controller (`com.dental.controller`)**:
  * Nhận HTTP Request, validate `@Valid` DTO, gọi Service tương ứng và trả về `ResponseEntity<?>`.
  * Không chứa logic nghiệp vụ tính toán hay logic truy vấn cơ sở dữ liệu.
* **Tầng Service (`com.dental.service`)**:
  * Chứa toàn bộ nghiệp vụ xử lý (tính khung giờ trống, kiểm tra trùng lịch, luồng trạng thái).
  * **Không import** các lớp thuộc tầng web (như `HttpServletRequest`, `HttpSession`) hoặc thư viện DB đặc thù.
  * Độc lập, dễ dàng Unit Test.
* **Tầng Repository (`com.dental.repository`)**:
  * Kế thừa `JpaRepository` của Spring Data JPA.
  * Chỉ đảm nhiệm việc truy vấn, lọc dữ liệu từ CSDL.

## 2. Quản lý DTO & Validation
* Không bao giờ trả trực tiếp Entity ra ngoài API. Luôn ánh xạ qua Response DTO.
* Mọi Request DTO tạo mới/cập nhật phải có annotation xác thực:
  * `@NotBlank`, `@NotNull`, `@Size(min = ..., max = ...)`
  * `@Email`, `@Pattern(regexp = ...)` cho số điện thoại
  * `@Future` / `@FutureOrPresent` cho ngày giờ
* Cấu hình Jackson không nhận các trường lạ (`fail-on-unknown-properties: true`).

## 3. Bảo mật (Spring Security & JWT)
* Luôn bảo vệ các endpoint nhạy cảm thông qua `SecurityFilterChain`.
* Xác thực không lưu trạng thái (Stateless), sử dụng `JwtAuthFilter` trích xuất token `Bearer <token>`.
* Phân quyền rõ ràng:
  * Public: `/api/clinic`, `/api/services`, `/api/doctors`, `/api/appointments/**`, `/api/auth/login`.
  * DOCTOR: `/api/doctor/**`.
  * ADMIN: `/api/admin/**`.
* Luôn kiểm tra tính hợp lệ của phiên đăng nhập (`login_sessions`) trước khi gán SecurityContext.

## 4. Xử lý ngoại lệ (Exception Handling)
* Sử dụng `@RestControllerAdvice` tập trung để bắt các exception:
  * `MethodArgumentNotValidException` (400 - Validation errors)
  * `BadCredentialsException`, `AuthenticationException` (401)
  * `AccessDeniedException` (403)
  * Custom exceptions: `ResourceNotFoundException` (404), `ConflictException` (409)
* Định dạng response lỗi chuẩn và nhất quán:
  ```json
  {
    "timestamp": "2026-10-13T21:00:00",
    "status": 409,
    "message": "Doctor already has an overlapping appointment",
    "errors": {}
  }
  ```
