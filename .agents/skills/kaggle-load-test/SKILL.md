---
name: kaggle-load-test
description: >-
  Sử dụng skill này khi cần viết kịch bản kiểm thử tải (Load Testing bằng Locust / k6)
  và chạy đo lường hiệu năng trên môi trường Kaggle CPU theo yêu cầu đề bài.
---

# Kaggle Load Testing Skill

Quy trình thiết kế kịch bản và chạy kiểm thử tải trên môi trường **Kaggle CPU** cho hệ thống Dental Clinic Backend.

## 1. Lựa chọn công cụ: Python Locust
Locust là lựa chọn tối ưu khi chạy trên Kaggle vì:
* Dễ cài đặt trực tiếp trong Jupyter Notebook (`!pip install locust`).
* Hỗ trợ viết kịch bản test hoàn toàn bằng Python.
* Chế độ **Headless Mode** cho phép xuất kết quả ra file CSV và vẽ biểu đồ phân tích trực tiếp trên Notebook.

## 2. Kịch bản test mẫu (`locustfile.py`)
Mô phỏng hành vi thực tế của khách hàng:
```python
import random
import uuid
from locust import HttpUser, task, between

class DentalClinicUser(HttpUser):
    wait_time = between(1, 2.5)

    @task(3)
    def view_catalog(self):
        """Khách xem phòng khám và dịch vụ"""
        self.client.get("/api/clinic", name="GET /api/clinic")
        self.client.get("/api/services", name="GET /api/services")
        self.client.get("/api/doctors", name="GET /api/doctors")

    @task(2)
    def check_available_slots(self):
        """Khách kiểm tra khung giờ trống"""
        self.client.get(
            "/api/doctors/1/available-slots?date=2030-01-02&serviceId=2",
            name="GET /api/doctors/{id}/available-slots"
        )

    @task(1)
    def book_and_lookup_appointment(self):
        """Khách đặt lịch và tra cứu lại"""
        payload = {
            "patientName": f"Patient_{uuid.uuid4().hex[:6]}",
            "phone": "0912345678",
            "email": "test@example.com",
            "serviceId": 2,
            "preferredDate": "2030-01-02",
            "preferredTime": "09:00",
            "note": "Load test"
        }
        res = self.client.post("/api/appointments", json=payload, name="POST /api/appointments")
        if res.status_code == 201:
            code = res.json().get("code")
            if code:
                self.client.get(f"/api/appointments/{code}", name="GET /api/appointments/{code}")
```

## 3. Cách chạy trên Kaggle Notebook CPU
Trong Kaggle Notebook:
1. Tải ứng dụng hoặc chạy background backend trong Kaggle:
   ```bash
   !nohup java -jar dental-clinic.jar > server.log 2>&1 &
   !sleep 5
   ```
2. Chạy Locust ở chế độ headless:
   ```bash
   !locust -f locustfile.py --host http://localhost:8080 --headless \
       -u 50 -r 5 --run-time 2m --csv=load_test_report
   ```
3. Đọc dữ liệu `load_test_report_stats.csv` bằng pandas và vẽ biểu đồ:
   * Requests Per Second (RPS)
   * 95th Percentile Response Time (Latency ms)
   * Error Rate (%)
