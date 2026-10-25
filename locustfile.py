"""SQLite baseline: customer/admin/doctor users with weights 7/2/1.

Weights select user classes and tasks, not percentages of HTTP requests.
Use scripts/run_load_test.py to seed, preflight and collect reproducible results.
"""
import os
import uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from locust import HttpUser, between, task
from locust.exception import StopUser

DATE = (datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")) + timedelta(days=7)).date().isoformat()
SERVICE_ID = int(os.environ.get("LOAD_SERVICE_ID", "1"))
DOCTOR_ID = int(os.environ.get("LOAD_DOCTOR_ID", "1"))


def check_json(response, expected, predicate):
    if response.status_code != expected:
        response.failure(f"Expected HTTP {expected}, got {response.status_code}")
        return None
    try:
        data = response.json()
        if not predicate(data):
            response.failure("Response does not match API contract")
            return None
        return data
    except (ValueError, TypeError, KeyError):
        response.failure("Invalid JSON response")
        return None


class CustomerUser(HttpUser):
    weight = 7
    wait_time = between(1, 2.5)

    @task(6)
    def catalog(self):
        for endpoint in ("clinic", "services", "doctors"):
            with self.client.get(f"/api/{endpoint}", catch_response=True) as response:
                check_json(response, 200, lambda d: isinstance(d, (dict, list)) and bool(d))

    @task(3)
    def available_slots(self):
        with self.client.get(
            f"/api/doctors/{DOCTOR_ID}/available-slots?date={DATE}&serviceId={SERVICE_ID}",
            name="/api/doctors/{id}/available-slots", catch_response=True,
        ) as response:
            check_json(response, 200, lambda d: isinstance(d.get("slots"), list))

    @task(1)
    def book_lookup_cancel(self):
        payload = {
            "patientName": "Synthetic " + uuid.uuid4().hex[:12], "phone": "0912345678",
            "serviceId": SERVICE_ID, "preferredDate": DATE, "preferredTime": "09:00",
            "note": "Synthetic load test data",
        }
        with self.client.post("/api/appointments", json=payload, catch_response=True) as response:
            data = check_json(response, 201, lambda d: d.get("status") == "PENDING" and bool(d.get("code")))
        if data is None:
            return
        code = data["code"]
        with self.client.get(f"/api/appointments/{code}", name="/api/appointments/{code}", catch_response=True) as response:
            check_json(response, 200, lambda d: d.get("code") == code)
        with self.client.delete(f"/api/appointments/{code}", name="/api/appointments/{code}", catch_response=True) as response:
            check_json(response, 200, lambda d: d.get("status") == "CANCELLED")


class AuthenticatedUser(HttpUser):
    abstract = True
    wait_time = between(1, 2.5)
    username = ""
    password = ""

    def on_start(self):
        with self.client.post("/api/auth/login", json={"username": self.username, "password": self.password}, catch_response=True) as response:
            data = check_json(response, 200, lambda d: bool(d.get("accessToken")))
        if data is None:
            raise StopUser()
        self.client.headers.update({"Authorization": "Bearer " + data["accessToken"]})


class AdminUser(AuthenticatedUser):
    weight = 2
    username = "admin"
    password = "Admin123!"  # Explicit demo/kaggle seed account; never a production credential.

    @task
    def appointments(self):
        with self.client.get("/api/admin/appointments", catch_response=True) as response:
            check_json(response, 200, lambda d: isinstance(d, list))

    @task
    def doctors(self):
        with self.client.get("/api/admin/doctors", catch_response=True) as response:
            check_json(response, 200, lambda d: isinstance(d, list) and bool(d))


class DoctorUser(AuthenticatedUser):
    weight = 1
    username = "doctor1"
    password = "Doctor123!"

    @task
    def appointments(self):
        with self.client.get("/api/doctor/appointments", catch_response=True) as response:
            check_json(response, 200, lambda d: isinstance(d, list))

    @task
    def profile(self):
        with self.client.get("/api/doctor/profile", catch_response=True) as response:
            check_json(response, 200, lambda d: bool(d.get("id")) and d.get("active") is True)
