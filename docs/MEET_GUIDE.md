# Dàn ý Google Meet (6 phút 30 giây)

Chia sẻ README; không đọc hết mã nguồn hoặc toàn bộ bảng API.

| Thời gian | Nội dung | Mở gì |
| --- | --- | --- |
| 0:00–0:45 | Bài toán tiếp nhận yêu cầu khám, bố trí bác sĩ và tránh chồng giờ; ba vai trò. | README, mục 1 |
| 0:45–1:45 | Khách gửi PENDING → Admin phân ASSIGNED → bác sĩ xác nhận/hoàn thành. Giờ mong muốn chưa giữ chỗ, ca liền kề được phép. | README, mục 2 |
| 1:45–2:45 | Controller → Service → Repository → SQLite. Chỉ vị trí AppointmentService, ScheduleService và schema.sql. | README, mục 3 |
| 2:45–3:45 | Java/Spring/JPA/JWT; DTO, filter, BCrypt, thu hồi phiên; GET/POST có xác thực, Docker, Swagger. | OpenAPI preview |
| 3:45–5:15 | Một Admin, tài khoản bác sĩ riêng, đăng nhập/làm nóng trước đo. Đọc bảng kết quả mới, chỉ ra API chậm nếu có. | README, mục 5; notebook có output |
| 5:15–6:00 | Pool 1, chưa phân trang, một Admin có thể làm hàng chờ tăng; giới hạn phép đo. | README, mục 6 |
| 6:00–6:30 | Chốt phạm vi; chỉ file JSON và notebook đã chạy trong GitHub. | Link bằng chứng |

## Ba câu cần giải thích được

- **Hai người đặt cùng giờ có bị trùng không?** PENDING chưa có bác sĩ. Trùng được kiểm tra khi phân công theo cùng bác sĩ/ngày và khoảng thời gian giao nhau; SQLite trigger kiểm tra lại lúc ghi.
- **JWT sao đăng xuất được ngay?** Token gắn mã phiên; mỗi request kiểm tra phiên và tài khoản còn hoạt động. Đăng xuất thu hồi phiên đó.
- **500 khách có phải 500 request cùng lúc không?** Không. Các phiên nghỉ 1–2,5 giây giữa tác vụ. Tổng phiên = khách + bác sĩ + 1 Admin; xem phases.json/CSV.

## Chuẩn bị trước buổi Meet

1. Mở sẵn README, OpenAPI preview và notebook có output trên GitHub; không phụ thuộc notebook Kaggle riêng tư.
2. Chạy Docker hoặc Maven và kiểm tra Swagger. Chỉ dùng dữ liệu giả lập.
3. Nếu demo, dùng docs/api.http: đặt lịch → đăng nhập Admin → phân công → thử phân công chồng giờ nhận 409. Dùng ngày tương lai.
4. Nêu đúng commit trong metadata; không coi kết quả cũ 7/2/1 là kết quả của kịch bản một Admin.
