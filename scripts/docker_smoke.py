"""Build an isolated Compose project, exercise API rules and verify SQLite persistence."""
import json
import os
import secrets
import socket
import subprocess
import time
import urllib.error
from datetime import datetime, timezone, timedelta
from pathlib import Path

from run_load_test import ROOT, preflight, request


def main():
    project = "ktpm-phase1-" + secrets.token_hex(4)
    destination = ROOT / "reports" / project
    destination.mkdir(parents=True)
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    env = {**os.environ, "HOST_PORT": str(port), "JWT_SECRET": secrets.token_hex(32)}
    base = f"http://127.0.0.1:{port}"
    prefix = ["docker", "compose", "-p", project]
    def docker(*args, check=True):
        with (destination / "docker.log").open("a", encoding="utf-8") as log:
            return subprocess.run(prefix + list(args), cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, check=check)
    def ready():
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            try:
                request(base, "GET", "/api/clinic")
                return
            except (urllib.error.URLError, ConnectionError, TimeoutError):
                time.sleep(.5)
        raise RuntimeError("Docker backend did not become ready")
    result = {"project": project, "database": "SQLite", "status": "starting"}
    try:
        docker("up", "--build", "--detach")
        ready()
        _, _, checks = preflight(base)
        result.update(checks)
        future = (datetime.now(timezone(timedelta(hours=7))) + timedelta(days=7)).date().isoformat()
        service = request(base, "GET", "/api/services")[0]["id"]
        booking = request(base, "POST", "/api/appointments", {"patientName": "Persistence Synthetic", "phone": "0912345678", "serviceId": service, "preferredDate": future, "preferredTime": "11:00"}, expected=201)
        docker("restart", "backend")
        ready()
        persisted = request(base, "GET", "/api/appointments/" + booking["code"])
        assert persisted["id"] == booking["id"] and persisted["status"] == "PENDING"
        result["sqlite_persistence_after_restart"] = "PASS"
        result["status"] = "passed"
    finally:
        docker("logs", "--no-color", check=False)
        docker("down", "--volumes", check=False)
        (destination / "result.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
