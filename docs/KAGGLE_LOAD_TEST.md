# Kaggle CPU / SQLite: một Admin cố định

## Kịch bản nộp bài

| Admin | Bác sĩ (tài khoản riêng) | Khách hàng | Tổng phiên |
| ---: | ---: | ---: | ---: |
| 1 | 5 | 100 | 106 |
| 1 | 10 | 200 | 211 |
| 1 | 20 | 500 | 521 |

Đây là các mức tải mô phỏng, không khẳng định quy mô nhân sự của phòng khám thực tế. Các lớp Locust dùng fixed_count, không chia theo trọng số 7/2/1. Mỗi bác sĩ có username riêng, một ca được phân công ban đầu và chỉ cập nhật lịch của mình.

Khách đọc danh mục/lịch trống, đặt–tra cứu; 20% yêu cầu mới hủy ngay. Một Admin đọc danh sách và phân công yêu cầu PENDING. Bác sĩ đọc, xác nhận và hoàn thành lịch. Lượt đọc và ghi đều kiểm tra HTTP/JSON; lỗi nghiệp vụ ngoài dự kiến cũng được tính lỗi, không bỏ qua.

## Đo ổn định

1. Backend/DB mới cho mỗi mức; preflight kiểm tra quyền, trùng giờ, chuyển trạng thái và JWT.
2. Tạo đủ số phiên; chờ mọi bác sĩ/Admin đăng nhập xong, rồi làm nóng 15 giây. Tốc độ tạo phiên 50/giây.
3. Tạm dừng phát request, chờ request đang chạy kết thúc, lưu setup CSV và reset thống kê.
4. Giữ nguyên các phiên, chạy 120 giây với thời gian nghỉ 1–2,5 giây giữa tác vụ. CSV kiểm tra đúng 1 Admin và đủ bác sĩ/khách mỗi giây.
5. Dừng phát request và chờ phần đang xử lý kết thúc. phases.json ghi thời gian đo thực tế; RPS tổng hợp = request / thời gian này. CSV gốc của Locust vẫn được giữ.

Login thuộc setup_stats.csv, không thuộc bảng tải ổn định. Chỉ tiêu tỷ lệ lỗi <=1% là lựa chọn của nhóm. Hàng chờ có thể tăng vì chỉ có một Admin phân công; tốc độ tạo yêu cầu không phải tốc độ hoàn thành ca khám.

## Chạy Kaggle và lưu notebook thật

Import notebooks/phase1_sqlite_kaggle.ipynb, chọn Accelerator None và Internet On. Save Version → Save & Run All. Notebook build/verify JDK 17, chạy notebook con bằng nbclient và lưu executed-fixed-admin.ipynb có execution_count/outputs thực tế, cùng ZIP kết quả. Lấy ZIP từ tab Output, không cần API token Kaggle.

Nguồn mặc định là nhánh codex/phase1-sqlite-kaggle; commit thực tế được ghi trong metadata. Notebook đã chạy trong gói bằng chứng cố định đúng commit đó. Để tái lập, dùng commit ghi trong kết quả thay vì mặc nhiên lấy mã mới nhất.

## Chạy thử tại máy (không coi là bằng chứng Kaggle)

    ./mvnw -B verify
    python -m pip install -r requirements-load.txt
    python scripts/run_load_test.py --environment local --customers 10 --doctors 3 --spawn-rate 10 --warmup 2 --duration 10 --output reports/local-fixed-admin

Mỗi output phải là thư mục mới. Backend dùng SQLite pool 1, journal delete, heap 512 MiB. Không có tối ưu DB cho Pha 2. Backend và Locust chia sẻ máy; đọc CPU/RAM/quota thực tế trong metadata.json, không suy ra tải tối đa hoặc số người dùng/ngày. Một lần mỗi mức chưa cho biết độ dao động giữa các lần lặp.

Kết quả cũ 7/2/1 trong evidence/kaggle-20261025 là lịch sử của workload khác, không dùng làm so sánh trước/sau tối ưu. Kết quả mới, notebook đã chạy và metadata được liên kết từ README sau khi hoàn tất.
