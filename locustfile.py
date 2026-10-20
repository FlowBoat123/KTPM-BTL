import uuid
from locust import HttpUser, task, between

class DentalClinicUser(HttpUser):
    """
    Kịch bản kiểm thử tải mô phỏng hành vi thực tế của người dùng:
    - 60% lượt xem danh mục (phòng khám, dịch vụ, bác sĩ)
    - 25% lượt xem khung giờ trống của bác sĩ
    - 15% lượt đặt lịch hẹn và tra cứu lại lịch hẹn
    """
    wait_time = between(1, 2.5)

    @task(6)
    def view_catalog(self):
        """Khách hàng xem thông tin phòng khám, danh mục dịch vụ và danh sách bác sĩ"""
        self.client.get("/api/clinic", name="GET /api/clinic")
        self.client.get("/api/services", name="GET /api/services")
        self.client.get("/api/doctors", name="GET /api/doctors")

    @task(3)
    def check_available_slots(self):
        """Khách hàng kiểm tra khung giờ trống của bác sĩ theo ngày và dịch vụ"""
        self.client.get(
            "/api/doctors/1/available-slots?date=2030-01-02&serviceId=2",
            name="GET /api/doctors/1/available-slots"
        )

    @task(1)
    def book_and_lookup_appointment(self):
        """Khách hàng đặt lịch hẹn mới và tra cứu lại bằng mã booking code"""
        unique_id = uuid.uuid4().hex[:6]
        payload = {
            "patientName": f"Patient_{unique_id}",
            "phone": "0912345678",
            "email": f"patient_{unique_id}@example.com",
            "serviceId": 2,
            "preferredDate": "2030-01-02",
            "preferredTime": "09:00",
            "note": "Load test appointment"
        }
        res = self.client.post("/api/appointments", json=payload, name="POST /api/appointments")
        if res.status_code == 201:
            data = res.json()
            code = data.get("code")
            if code:
                self.client.get(f"/api/appointments/{code}", name="GET /api/appointments/{code}")
