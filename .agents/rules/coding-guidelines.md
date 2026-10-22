# Coding Guidelines & Project Rules (Dental Clinic Backend)

1. **Phân tầng nghiêm ngặt**:
   - Controller: Chỉ làm việc với DTO, validation và trả HTTP Status thích hợp.
   - Service: Chứa business logic, độc lập hoàn toàn với web framework và SQL syntax.
   - Repository: Chỉ dùng Spring Data JPA interfaces.
2. **DTO & Serialization**:
   - Không trả Entity trực tiếp ra API.
   - Mọi request DTO đều phải được chú thích validation đầy đủ (`@NotBlank`, `@NotNull`,...).
3. **Bảo mật**:
   - Tất cả endpoint quản trị và bác sĩ phải đi qua Spring Security JWT Filter.
   - Không hardcode bí mật (dùng biến môi trường với giá trị fallback an toàn).
4. **Tài liệu hóa**:
   - Khi thêm API mới, luôn cập nhật cả `docs/api.http` và OpenAPI annotations (`@Operation`, `@ApiResponse`).
