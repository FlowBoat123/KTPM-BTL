# PROJECT CONTEXT: ĐỒ ÁN KIẾN TRÚC PHẦN MỀM (KTPM-BTL 2026) - PHA 1

> **Mục đích file**: Lưu trữ ngữ cảnh dự án tập trung vào **Pha 1**. Khi bắt đầu phiên làm việc mới, bạn chỉ cần yêu cầu:  
> *"Đọc file `PROJECT_CONTEXT.md` để nắm lại ngữ cảnh bài toán."*

---

## 1. PHẠM VI DỰ ÁN: CHỈ THỰC HIỆN PHA 1

Theo yêu cầu từ bạn, dự án **chỉ tập trung hoàn thiện đầy đủ và chuẩn xác các tiêu chí của Pha 1**, không triển khai các kiến trúc phức tạp của Pha 2.

### Tiêu chí nghiệm thu Pha 1 (Theo `docs/2026.md`)
1. **Nghiệp vụ backend**: Dental Clinic Backend (đặt lịch phòng khám nha khoa).
2. **REST API (JSON)**:
   * Hỗ trợ đầy đủ các method: `GET`, `POST`, `DELETE` (và `PATCH`, `PUT`).
   * **Bắt buộc có tài liệu OpenAPI / Swagger UI**.
3. **Kiến trúc phân tầng chuẩn**:
   * `API (Controller)` → `Nghiệp vụ (Service)` → `Truy cập dữ liệu (Repository)`.
   * Tầng nghiệp vụ không phụ thuộc framework web hay thư viện DB.
   * Truy cập dữ liệu qua Repository (Spring Data JPA).
4. **Bảo mật & Phân quyền**:
   * Hỗ trợ đăng nhập (Login JWT).
   * Có ít nhất 1 endpoint `GET` và 1 endpoint `POST` yêu cầu xác thực.
   * Xác thực xử lý tập trung bằng filter/middleware của framework (`JwtAuthFilter`), không lặp code.
5. **Đóng gói & Triển khai**:
   * **Hỗ trợ đóng gói bằng Docker** (`Dockerfile`, `.dockerignore`, `docker-compose.yml`).
6. **Tài liệu & Kiểm thử**:
   * `README.md` mô tả kiến trúc và đặc tả chi tiết.
   * **Kịch bản kiểm thử tải trên Kaggle CPU** (k6 / Locust).

---

## 2. HIỆN TRẠNG KỸ THUẬT & NGHIỆP VỤ (PHA 1)

### 2.1. Công nghệ sử dụng
* **Ngôn ngữ & Runtime**: Java 17+, Maven Wrapper.
* **Framework**: Spring Boot 3.5.16, Spring Web, Spring Data JPA, Spring Security, Validation.
* **Cơ sở dữ liệu**: SQLite (`dental-clinic.db`) với driver Xerial SQLite JDBC & SQLiteDialect Hibernate.
* **Xác thực**: JWT (Nimbus Jose) + Session tracking trong SQLite (`login_sessions`).

### 2.2. Nghiệp vụ & Các nhóm người dùng
1. **Khách hàng (Public)**:
   * Xem thông tin phòng khám (`/api/clinic`), danh mục dịch vụ (`/api/services`), bác sĩ (`/api/doctors`).
   * Xem khung giờ còn trống (`/api/doctors/{id}/available-slots`).
   * Đặt lịch khám (`POST /api/appointments`) -> sinh mã bí mật `appointmentCode`, trạng thái `PENDING`.
   * Tra cứu lịch khám bằng mã (`GET /api/appointments/{code}`).
   * Hủy lịch khám trước giờ bắt đầu (`DELETE /api/appointments/{code}`).
2. **Admin (Quyền ADMIN)**:
   * Đăng nhập (`POST /api/auth/login`).
   * Xem toàn bộ lịch hẹn (`/api/admin/appointments`).
   * Phân công bác sĩ, ngày & giờ khám thực tế (`PATCH /api/admin/appointments/{id}/assign`).
   * Thay đổi trạng thái lịch hẹn (`PATCH /api/admin/appointments/{id}/status`).
   * Quản lý bác sĩ: Xem, tạo mới (`POST /api/admin/doctors`), cập nhật, vô hiệu hóa (`DELETE /api/admin/doctors/{id}`).
3. **Bác sĩ (Quyền DOCTOR)**:
   * Đăng nhập (`POST /api/auth/login`).
   * Xem lịch hẹn được giao (`/api/doctor/appointments`).
   * Xác nhận / Hoàn thành / Hủy lịch (`PATCH /api/doctor/appointments/{id}/status` -> `CONFIRMED`, `COMPLETED`, `CANCELLED`).
   * Quản lý hồ sơ bác sĩ (`/api/doctor/profile`).

### 2.3. Quy tắc cốt lõi
* Trạng thái lịch: `PENDING` → `ASSIGNED` → `CONFIRMED` → `COMPLETED` (hoặc `CANCELLED`).
* Khách không chọn bác sĩ; Admin phân công bác sĩ và giờ khám.
* Múi giờ: `Asia/Ho_Chi_Minh`. Ngày/giờ ở tương lai, bước 30 phút, trong giờ làm việc (08:00 - 17:00).
* Chống trùng lịch ca khám của cùng bác sĩ bằng Service logic + Trigger SQLite.
* Không vô hiệu hóa bác sĩ nếu còn lịch mở (`ASSIGNED` / `CONFIRMED`).

---

## 3. CHECKLIST CÔNG VIỆC CẦN LÀM ĐỂ HOÀN THIỆN TRỌN VẸN PHA 1

| Hạng mục | Tình trạng | Chi tiết thực hiện |
| :--- | :---: | :--- |
| **1. Nghiệp vụ & API** | ✅ **Đã hoàn thành** | 26 endpoints RESTful chuẩn, giao tiếp JSON, đầy đủ GET/POST/DELETE/PUT/PATCH. |
| **2. Phân tầng & Security** | ✅ **Đã hoàn thành** | API (Controller) → Service (không leak framework/DB) → Repository (Spring Data JPA / Hibernate). Spring Security JWT filter. |
| **3. OpenAPI / Swagger UI** | ✅ **Đã hoàn thành** | Tích hợp SpringDoc OpenAPI 3 (`/v3/api-docs`), giao diện Swagger UI tương tác (`/swagger-ui/index.html`), hỗ trợ BearerAuth JWT. |
| **4. Đóng gói Docker** | ✅ **Đã hoàn thành** | `Dockerfile` (multi-stage Temurin 17 JRE Alpine), `.dockerignore`, `docker-compose.yml` (Backend + SQLite persistent volume). |
| **5. Kiểm thử tải Kaggle CPU** | **Xem bằng chứng nghiệm thu** | Notebook, runner, CSV/HTML/metadata và kết quả thực thi được liên kết tại `docs/PHASE1_ACCEPTANCE.md`. |
| **6. Cập nhật README.md** | ✅ **Đã hoàn thành** | Đầy đủ kiến trúc, đặc tả, Swagger, Docker SQLite và hướng dẫn chạy kiểm thử tải. |

---

## 4. HƯỚNG DẪN CHẠY NHANH LOCAL

* **Khởi chạy ứng dụng**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
* **Tài khoản mặc định (Demo)**:
  * Admin: `admin` / `Admin123!`
  * Bác sĩ: `doctor1` / `Doctor123!`
* **Kiểm thử API**: Mở file `docs/api.http` trong VS Code / IntelliJ.
