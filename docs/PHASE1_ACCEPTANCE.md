# Đối chiếu nghiệm thu Pha 1

Nguồn yêu cầu: [2026.md](https://github.com/maytinhdibo/KTPM-architecture-solution/blob/main/2026.md).

| Yêu cầu | Hiện thực / bằng chứng |
| --- | --- |
| REST JSON, GET/POST/DELETE | 26 endpoint; `docs/api.http`, DTO và test tích hợp. |
| OpenAPI/Swagger | `/v3/api-docs`, `/swagger-ui/index.html`; test truy cập không cần token. |
| API → nghiệp vụ → dữ liệu; nghiệp vụ không import web/DB library | BusinessException không chứa HTTP; ánh xạ HTTP tại handler; ArchitectureTest kiểm tra phụ thuộc. |
| Repository + ORM | Spring Data JPA / Hibernate và SQLiteDialect. |
| Đăng nhập, GET và POST có xác thực, xử lý tập trung | JWT filter; GET `/api/admin/appointments`, POST `/api/admin/doctors`; kiểm thử 401/403/quyền sở hữu. |
| Docker | Dockerfile + Compose SQLite; `scripts/docker_smoke.py` kiểm tra API và giữ dữ liệu sau restart; CI lưu artifact. |
| GitHub public + README kiến trúc/đặc tả | README, tài liệu API, PR và GitHub Actions. |
| Kaggle CPU | Notebook `notebooks/phase1_sqlite_kaggle.ipynb`; trạng thái thực thi được cập nhật bên dưới. |

## Kiểm tra mã nguồn và Docker

- `mvnw verify`: 18 test, không có lỗi hoặc bỏ qua (16 API, 1 kiến trúc, 1 profile local).
- Bài đo baseline chọn SQLite, pool 1, Java heap 512 MiB. PostgreSQL không phải yêu cầu bắt buộc và không dùng để so sánh baseline Pha 1 này.
- Docker build và smoke test đã đạt ngày 25/10/2026: đặt lịch → phân công → hoàn thành, quyền 401/403, trùng lịch 409, ghi chú dài và dữ liệu còn sau restart. CI chạy lại kiểm tra Docker và lưu artifact theo từng commit.

## Bằng chứng Kaggle

Đang chuẩn bị chạy; chưa ghi nhận số liệu Kaggle trong tài liệu này. Không sử dụng kết quả local thay cho yêu cầu Kaggle.

## Giới hạn

Kịch bản tải mô phỏng Customer/Admin/Doctor và có preflight các luồng chính. Không tuyên bố đã đo mọi API hoặc phân công đồng thời ở tải lớn. Backend và load generator cùng dùng tài nguyên máy Kaggle. Ngưỡng lỗi ≤1% là chỉ tiêu nhóm tự chọn; đề bài không ấn định số users, RPS hay độ trễ.
