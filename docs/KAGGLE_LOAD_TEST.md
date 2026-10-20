# HƯỚNG DẪN KIỂM THỬ TẢI TRÊN KAGGLE CPU (PHA 1)

Tài liệu hướng dẫn triển khai và thực hiện đo lường hiệu năng tải hệ thống Dental Clinic Backend trên môi trường **Kaggle Notebook (CPU)** bằng công cụ **Python Locust**.

---

## 1. Môi trường kiểm thử trên Kaggle CPU
- **Phần cứng**: Kaggle CPU VM (4 vCPU, 30 GB RAM).
- **Mục tiêu**: Đo lường thông lượng (Requests Per Second - RPS), độ trễ (Latency P95, Latency Mean) và tỷ lệ lỗi (Error Rate %) ở tải cơ bản của Pha 1 làm mốc cơ sở (baseline) so sánh cho Pha 2.

---

## 2. Các bước thực hiện trong Kaggle Notebook

### Bước 1: Chuẩn bị môi trường & cài đặt Locust
Tạo 1 notebook mới trên Kaggle (chọn Accelerator: **None (CPU)**), trong cell code đầu tiên:

```python
# Cài đặt thư viện Locust và các công cụ vẽ biểu đồ
!pip install locust pandas matplotlib seaborn -q
```

### Bước 2: Tải mã nguồn hoặc file JAR ứng dụng
Có thể clone trực tiếp repo hoặc upload file `dental-clinic-0.0.1-SNAPSHOT.jar`:

```bash
# Clone repo
!git clone https://github.com/FlowBoat123/KTPM-BTL.git
%cd KTPM-BTL

# Cài đặt OpenJDK 17 trong Kaggle container
!apt-get update -qq && apt-get install -y openjdk-17-jdk-headless -qq

# Build file JAR (nếu chưa có sẵn JAR)
!chmod +x mvnw
!./mvnw clean package -DskipTests
```

### Bước 3: Khởi chạy Backend ngầm trong Kaggle
```bash
# Chạy Spring Boot backend ở chế độ background
!nohup java -jar target/dental-clinic-0.0.1-SNAPSHOT.jar > server.log 2>&1 &
!sleep 15

# Kiểm tra log xem server đã sẵn sàng chưa
!tail -n 15 server.log
!curl -s http://localhost:8080/api/clinic
```

### Bước 4: Chạy kiểm thử tải với Locust (Headless Mode)
Chạy script kiểm thử tải [locustfile.py](../locustfile.py) trong 2 phút với 50 người dùng đồng thời, tốc độ sinh 5 users/giây:

```bash
!locust -f locustfile.py \
    --host http://localhost:8080 \
    --headless \
    --users 50 \
    --spawn-rate 5 \
    --run-time 2m \
    --csv=load_test_results \
    --csv-full-history
```

### Bước 5: Phân tích & Trực quan hóa kết quả
Trong cell Python tiếp theo:

```python
import pandas as pd
import matplotlib.pyplot as plt

# Đọc kết quả thống kê
stats = pd.read_csv('load_test_results_stats.csv')
print("=== TỔNG KẾT HIỆU NĂNG TẢI ===")
print(stats[['Name', 'Request Count', 'Failure Count', 'Median Response Time', 'Average Response Time', '95%']].to_string())

# Vẽ biểu đồ thời gian phản hồi P95
plt.figure(figsize=(10, 5))
plt.barh(stats['Name'][:-1], stats['95%'][:-1], color='skyblue')
plt.xlabel('Thời gian phản hồi P95 (ms)')
plt.title('Độ trễ P95 của các Endpoint trên Kaggle CPU')
plt.tight_layout()
plt.savefig('p95_latency.png')
plt.show()
```

---

## 3. Tiêu chí đánh giá hoàn thành Pha 1
- Backend xử lý ổn định ở mức 50 concurrent users.
- Tỷ lệ lỗi (Failure Rate) $< 1\%$.
- Dữ liệu CSV thu được (`load_test_results_stats.csv`, `load_test_results_stats_history.csv`) dùng làm báo cáo nghiệm thu Pha 1 và làm số liệu gốc (baseline) để đề xuất kiến trúc cải tiến trong Pha 2.
