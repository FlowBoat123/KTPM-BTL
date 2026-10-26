"""Explicit populations: one admin, distinct doctors and scalable customers."""
import itertools
import json
import os
import random
import uuid
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from locust import HttpUser, between, task
from locust.exception import StopUser
from gevent.event import Event

CUSTOMERS = int(os.environ.get("LOAD_CUSTOMERS", "100"))
DOCTORS = int(os.environ.get("LOAD_DOCTORS", "5"))
SERVICE_ID = int(os.environ.get("LOAD_SERVICE_ID", "2"))
DOCTOR_IDS = json.loads(os.environ.get("LOAD_DOCTOR_IDS", "[1,2]"))
DATE = (datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")) + timedelta(days=7)).date()
READY = {"AdminUser": 0, "DoctorUser": 0, "CustomerUser": 0}
doctor_slots = itertools.count(1)
RUN_GATE = Event()
RUN_GATE.set()
IN_FLIGHT = 0


class ApiUser(HttpUser):
    abstract = True
    wait_time = between(1, 2.5)

    def call(self, *args, **kwargs):
        global IN_FLIGHT
        RUN_GATE.wait()
        IN_FLIGHT += 1
        try:
            return self._call(*args, **kwargs)
        finally:
            IN_FLIGHT -= 1

    def _call(self, method, path, payload=None, *, name=None, expected=200, valid=lambda d: True):
        with self.client.request(method, path, json=payload, name=name, catch_response=True, timeout=20) as response:
            if response.status_code != expected:
                response.failure(f"Expected HTTP {expected}, got {response.status_code}")
                return None
            try:
                data = response.json()
                if not valid(data):
                    response.failure("Response does not match API contract")
                    return None
                return data
            except (ValueError, TypeError, KeyError, AttributeError):
                response.failure("Invalid JSON response")
                return None

    def login(self, username, password):
        data = self.call("POST", "/api/auth/login", {"username": username, "password": password},
                         valid=lambda d: bool(d.get("accessToken")))
        if data is None:
            raise StopUser()
        self.client.headers.update({"Authorization": "Bearer " + data["accessToken"]})
        READY[type(self).__name__] += 1


class CustomerUser(ApiUser):
    fixed_count = CUSTOMERS

    def on_start(self):
        READY["CustomerUser"] += 1

    @task(6)
    def catalog(self):
        for endpoint in ("clinic", "services", "doctors"):
            self.call("GET", f"/api/{endpoint}", valid=lambda d: isinstance(d, (dict, list)) and bool(d))

    @task(3)
    def slots(self):
        doctor = random.choice(DOCTOR_IDS)
        self.call("GET", f"/api/doctors/{doctor}/available-slots?date={DATE}&serviceId={SERVICE_ID}",
                  name="/api/doctors/{id}/available-slots", valid=lambda d: isinstance(d.get("slots"), list))

    @task(1)
    def booking(self):
        data = self.call("POST", "/api/appointments", {
            "patientName": "Synthetic " + uuid.uuid4().hex[:12], "phone": "0912345678",
            "serviceId": SERVICE_ID, "preferredDate": DATE.isoformat(),
            "preferredTime": "09:00", "note": "Synthetic load data",
        }, expected=201, valid=lambda d: d.get("status") == "PENDING" and bool(d.get("code")))
        if data is None:
            return
        code = data["code"]
        self.call("GET", f"/api/appointments/{code}", name="/api/appointments/{code}",
                  valid=lambda d: d.get("code") == code)
        # Most bookings remain pending for the sole admin.
        if random.random() < .2:
            self.call("DELETE", f"/api/appointments/{code}", name="/api/appointments/{code}",
                      valid=lambda d: d.get("status") == "CANCELLED")


class AdminUser(ApiUser):
    fixed_count = 1

    def on_start(self):
        self.assignment_number = 0
        self.login("admin", "Admin123!")  # Disposable database only.

    @task(1)
    def doctors(self):
        self.call("GET", "/api/admin/doctors", valid=lambda d: isinstance(d, list) and bool(d))

    @task(2)
    def dispatch(self):
        appointments = self.call("GET", "/api/admin/appointments", valid=lambda d: isinstance(d, list))
        if appointments is None:
            return
        pending = next((a for a in appointments if a["status"] == "PENDING"), None)
        if pending is None:
            return
        doctor = DOCTOR_IDS[self.assignment_number % len(DOCTOR_IDS)]
        day = DATE + timedelta(days=self.assignment_number // (len(DOCTOR_IDS) * 8))
        self.assignment_number += 1
        slots = self.call("GET", f"/api/doctors/{doctor}/available-slots?date={day}&serviceId={pending['serviceId']}",
                          name="/api/doctors/{id}/available-slots", valid=lambda d: isinstance(d.get("slots"), list))
        if not slots or not slots["slots"]:
            return
        self.call("PATCH", f"/api/admin/appointments/{pending['id']}/assign", {
            "doctorId": doctor, "appointmentDate": day.isoformat(), "startTime": slots["slots"][0],
        }, name="/api/admin/appointments/{id}/assign", valid=lambda d: d.get("status") == "ASSIGNED")


class DoctorUser(ApiUser):
    fixed_count = DOCTORS

    def on_start(self):
        self.login(f"doctor{next(doctor_slots)}", "Doctor123!")

    @task(1)
    def profile(self):
        self.call("GET", "/api/doctor/profile", valid=lambda d: bool(d.get("id")) and d.get("active") is True)

    @task(2)
    def appointments(self):
        appointments = self.call("GET", "/api/doctor/appointments", valid=lambda d: isinstance(d, list))
        if appointments is None:
            return
        active = next((a for a in appointments if a["status"] in ("ASSIGNED", "CONFIRMED")), None)
        if active:
            status = "CONFIRMED" if active["status"] == "ASSIGNED" else "COMPLETED"
            self.call("PATCH", f"/api/doctor/appointments/{active['id']}/status", {"status": status},
                      name="/api/doctor/appointments/{id}/status", valid=lambda d: d.get("status") == status)
