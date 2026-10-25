# Kiểm thử Pha 1 — SQLite trên Kaggle CPU

Database baseline: **SQLite**. Không bật profile `postgres` cho bài đo này. Chọn CPU (Accelerator None), Internet On.

## Chạy notebook

1. Trên Kaggle Code, tạo notebook và chọn File → Import Notebook → Upload. Tải file [phase1_sqlite_kaggle.ipynb](../notebooks/phase1_sqlite_kaggle.ipynb).
2. Đặt Accelerator None (CPU), Internet On. Notebook mặc định lấy nhánh `codex/phase1-sqlite-kaggle`; mã commit thực tế được ghi vào metadata.
3. Run All. Notebook cài JDK 17 và dependency đã pin, chạy `mvnw verify`, rồi chạy benchmark.
4. Save Version → Save & Run All. Giữ link notebook/version và tải gói ZIP output; không chỉ chụp ảnh một vài con số.

## Cấu hình phép đo

- 50 người dùng đồng thời; tốc độ tăng 5 users/giây; warm-up 15 giây; đo 120 giây. Có thể sửa các biến ở notebook, nhưng phải ghi rõ.
- Java heap 512 MiB, SQLite pool 1, journal mặc định. Không áp dụng tối ưu Pha 2.
- User class Customer/Admin/Doctor có trọng số 7/2/1. Các task của khách gồm đọc danh mục, xem lịch trống, đặt/tra cứu/hủy lịch; Admin/Bác sĩ đăng nhập rồi đọc dữ liệu được phân quyền.
- Trọng số user và task không phải phần trăm HTTP request. Luồng phân công và kiểm tra quyền sở hữu được chạy ở preflight, không phải bài đo tranh chấp phân công đồng thời.
- Mỗi lần đo tạo file DB mới trong thư mục output riêng. Warm-up và đo chính dùng cùng DB; thống kê warm-up được lưu riêng. Khởi tạo lại môi trường tương tự ở các lần so sánh.
- Backend và Locust chạy trên cùng máy Kaggle. Ghi CPU/RAM/cgroup quota thực tế trong `metadata.json`; không mặc định mọi notebook đều có cùng cấu hình.

## Bằng chứng cần giữ

Notebook/version có output thực thi; `metadata.json`, `preflight.json`, các CSV, HTML, đồ thị P95, server/Locust logs và `REPORT.md`. Runner tự đóng gói ZIP, không đưa token hoặc database vào ZIP.

Ngưỡng lỗi ≤1% là mục tiêu do nhóm chọn, không phải yêu cầu định lượng của đề bài. Kết quả không đạt vẫn cần lưu trung thực và giải thích. Script dừng với lỗi nếu request không đúng hợp đồng hoặc phép đo không có request.

## Kiểm tra nhanh ở máy cá nhân

Python 3.11+, JDK 17+. Lệnh sau chỉ tạo **local smoke test**, không phải bằng chứng Kaggle:

```powershell
.\mvnw.cmd verify
python -m pip install -r requirements-load.txt
python scripts/run_load_test.py --environment local --users 10 --spawn-rate 5 --duration 20 --warmup 5 --output reports/local-smoke-01
```

Không ghi đè thư mục kết quả cũ; chọn tên output mới cho mỗi lần chạy. Sau khi chạy Kaggle, bổ sung kết quả vào [PHASE1_ACCEPTANCE.md](PHASE1_ACCEPTANCE.md).
