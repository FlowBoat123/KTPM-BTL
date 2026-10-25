"""Run an isolated SQLite benchmark. Local output is never labelled as Kaggle evidence."""
from __future__ import annotations

import argparse
import csv
import importlib.metadata
import json
import os
import platform
import secrets
import socket
import sqlite3
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CREATE_FLAGS = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def command(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, creationflags=CREATE_FLAGS, **kwargs)


def output_of(args):
    result = command(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.stdout.strip()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def stop(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def request(base, method, path, payload=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base + path, data=None if payload is None else json.dumps(payload).encode(), headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
    if status != expected:
        raise RuntimeError(f"{method} {path}: expected {expected}, got {status}: {body.decode()[:300]}")
    return json.loads(body) if body else None


def preflight(base):
    services = request(base, "GET", "/api/services")
    doctors = request(base, "GET", "/api/doctors")
    service = next(s for s in services if s["durationMinutes"] == 60)
    doctor = doctors[0]
    tomorrow = (datetime.now(timezone(timedelta(hours=7))) + timedelta(days=7)).date().isoformat()
    payload = {"patientName": "Preflight Synthetic", "phone": "0912345678", "serviceId": service["id"], "preferredDate": tomorrow, "preferredTime": "09:00", "note": "X" * 300}
    appointment = request(base, "POST", "/api/appointments", payload, expected=201)
    assert appointment["status"] == "PENDING" and appointment["doctorId"] is None
    request(base, "GET", "/api/admin/appointments", expected=401)
    request(base, "POST", "/api/admin/doctors", {}, expected=401)
    admin = request(base, "POST", "/api/auth/login", {"username": "admin", "password": "Admin123!"})["accessToken"]
    doctor_token = request(base, "POST", "/api/auth/login", {"username": "doctor1", "password": "Doctor123!"})["accessToken"]
    other_token = request(base, "POST", "/api/auth/login", {"username": "doctor2", "password": "Doctor123!"})["accessToken"]
    assigned = request(base, "PATCH", f"/api/admin/appointments/{appointment['id']}/assign", {"doctorId": doctor["id"], "appointmentDate": tomorrow, "startTime": "09:00"}, admin)
    assert assigned["status"] == "ASSIGNED" and assigned["endTime"] == "10:00:00"
    request(base, "GET", f"/api/doctor/appointments/{appointment['id']}", token=other_token, expected=403)
    second = request(base, "POST", "/api/appointments", payload, expected=201)
    request(base, "PATCH", f"/api/admin/appointments/{second['id']}/assign", {"doctorId": doctor["id"], "appointmentDate": tomorrow, "startTime": "09:30"}, admin, expected=409)
    done = request(base, "PATCH", f"/api/doctor/appointments/{appointment['id']}/status", {"status": "COMPLETED"}, doctor_token)
    assert done["status"] == "COMPLETED"
    request(base, "POST", "/api/auth/logout", token=other_token, expected=204)
    request(base, "GET", "/api/auth/me", token=other_token, expected=401)
    return service["id"], doctor["id"], {"booking_assignment_completion": "PASS", "long_note": "PASS", "authentication_authorization": "PASS", "overlap_rejected": "PASS", "logout_revocation": "PASS"}


def run_locust(base, destination, users, rate, seconds, env):
    destination.mkdir(parents=True, exist_ok=True)
    args = [sys.executable, "-m", "locust", "-f", str(ROOT / "locustfile.py"), "--host", base, "--headless", "--users", str(users), "--spawn-rate", str(rate), "--run-time", f"{seconds}s", "--stop-timeout", "15", "--csv", str(destination / "locust"), "--csv-full-history", "--html", str(destination / "report.html"), "--only-summary", "--exit-code-on-error", "0"]
    with (destination / "locust.log").open("w", encoding="utf-8") as log:
        result = command(args, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=seconds + 120)
    if result.returncode:
        raise RuntimeError(f"Locust exited with {result.returncode}; see {destination / 'locust.log'}")


def summarize(destination, metadata):
    with (destination / "locust_stats.csv").open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    total = next(row for row in rows if row["Name"] == "Aggregated")
    count, failures = int(total["Request Count"]), int(total["Failure Count"])
    if not count:
        raise RuntimeError("No requests recorded; this is not a valid benchmark")
    summary = {"requests": count, "failures": failures, "failure_ratio": failures / count, "rps": float(total["Requests/s"]), "mean_ms": float(total["Average Response Time"]), "p95_ms": float(total["95%"])}
    summary["target_met"] = summary["failure_ratio"] <= metadata["max_failure_ratio"]
    write_json(destination / "summary.json", summary)
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    endpoint_rows = [row for row in rows if row["Name"] != "Aggregated" and int(row["Request Count"]) > 0]
    labels = [row["Type"] + " " + row["Name"] for row in endpoint_rows]
    fig, ax = plt.subplots(figsize=(11, max(4, len(labels) * .4)))
    ax.barh(labels, [float(row["95%"]) for row in endpoint_rows])
    ax.set_xlabel("P95 response time (ms)")
    ax.set_title(f"SQLite baseline - {metadata['environment']}")
    fig.tight_layout()
    fig.savefig(destination / "p95_latency.png", dpi=160)
    plt.close(fig)
    lines = ["# SQLite performance baseline", "", f"Environment: **{metadata['environment']}**", f"Source commit: `{metadata['commit']}`", f"Users: {metadata['users']}; measurement: {metadata['duration_seconds']}s; warm-up: {metadata['warmup_seconds']}s.", "", "| Metric | Value |", "| --- | --- |"]
    lines += [f"| {key} | {value} |" for key, value in summary.items()]
    lines += ["", "Load generator and backend share the same host. JVM heap is fixed at 512 MiB. SQLite uses one application connection and its default journal mode; no Phase 2 tuning is applied.", "", "Task/user weights do not equal proportions of HTTP requests. This workload includes public reads and booking/lookup/cancellation writes plus authenticated Admin/Doctor reads. Assignment and ownership rules are checked before measurement, not benchmarked concurrently.", "", "The failure target is a project choice, not a threshold required by the assignment. A local smoke test is not Kaggle evidence."]
    (destination / "REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return summary


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--users", type=int, default=50)
    parser.add_argument("--spawn-rate", type=int, default=5)
    parser.add_argument("--duration", type=int, default=120)
    parser.add_argument("--warmup", type=int, default=15)
    parser.add_argument("--max-failure-ratio", type=float, default=.01)
    parser.add_argument("--environment", choices=["local", "kaggle"], default="local")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if min(args.users, args.spawn_rate, args.duration) < 1 or args.warmup < 0 or not 0 <= args.max_failure_ratio <= 1:
        parser.error("Invalid load settings")
    if args.environment == "kaggle" and not Path("/kaggle/working").is_dir():
        parser.error("Kaggle evidence must actually be generated in /kaggle/working")
    destination = args.output.resolve()
    if destination.exists():
        parser.error("Output directory already exists; use a new directory for each run")
    destination.mkdir(parents=True)
    jars = [p for p in (ROOT / "target").glob("dental-clinic-*.jar") if not p.name.endswith(".original")]
    if len(jars) != 1:
        parser.error("Build the application first: ./mvnw -B verify")
    jar = jars[0]
    with zipfile.ZipFile(jar) as archive:
        jdbc = next(name for name in archive.namelist() if name.startswith("BOOT-INF/lib/sqlite-jdbc-"))
    metadata = {"environment": args.environment, "utc_started": datetime.now(timezone.utc).isoformat(), "commit": output_of(["git", "rev-parse", "HEAD"]), "working_tree": output_of(["git", "status", "--porcelain"]), "platform": platform.platform(), "cpu_model": platform.processor(), "logical_cpus": os.cpu_count(), "database": "SQLite", "sqlite_jdbc": Path(jdbc).name, "connection_pool_size": 1, "jvm_heap_mib": 512, "java": output_of(["java", "-version"]), "python": sys.version, "locust": importlib.metadata.version("locust"), "matplotlib": importlib.metadata.version("matplotlib"), "users": args.users, "spawn_rate": args.spawn_rate, "duration_seconds": args.duration, "warmup_seconds": args.warmup, "max_failure_ratio": args.max_failure_ratio, "same_host_load_generator": True, "status": "starting"}
    for name, path in {"cpuinfo": "/proc/cpuinfo", "meminfo": "/proc/meminfo", "cgroup_cpu_max": "/sys/fs/cgroup/cpu.max", "cgroup_memory_max": "/sys/fs/cgroup/memory.max"}.items():
        if Path(path).is_file():
            metadata[name] = Path(path).read_text()[:16000]
    write_json(destination / "metadata.json", metadata)
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    base = f"http://127.0.0.1:{port}"
    env = {**os.environ, "SPRING_PROFILES_ACTIVE": "kaggle", "SERVER_ADDRESS": "127.0.0.1", "PORT": str(port), "DB_PATH": str(destination / "benchmark.db"), "JWT_SECRET": secrets.token_hex(32)}
    summary = None
    with (destination / "server.log").open("w", encoding="utf-8") as log:
        process = subprocess.Popen(["java", "-Xms512m", "-Xmx512m", "-jar", str(jar)], cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=CREATE_FLAGS)
        try:
            deadline = time.monotonic() + 120
            while True:
                if process.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError("Backend did not become ready; see server.log")
                try:
                    request(base, "GET", "/api/clinic")
                    break
                except (urllib.error.URLError, TimeoutError, ConnectionError):
                    time.sleep(.5)
            service, doctor, checks = preflight(base)
            write_json(destination / "preflight.json", checks)
            env.update(LOAD_SERVICE_ID=str(service), LOAD_DOCTOR_ID=str(doctor))
            if args.warmup:
                run_locust(base, destination / "warmup", args.users, args.spawn_rate, args.warmup, env)
            metadata["status"] = "measuring"
            write_json(destination / "metadata.json", metadata)
            run_locust(base, destination, args.users, args.spawn_rate, args.duration, env)
            summary = summarize(destination, metadata)
            metadata["status"] = "completed"
        except Exception as error:
            metadata["status"] = "failed"
            metadata["error"] = str(error)
            raise
        finally:
            stop(process)
            database = destination / "benchmark.db"
            if database.exists():
                with sqlite3.connect(database) as db:
                    metadata["journal_mode"] = db.execute("PRAGMA journal_mode").fetchone()[0]
                    metadata["appointment_rows_after_run"] = db.execute("SELECT count(*) FROM appointments").fetchone()[0]
            metadata["utc_finished"] = datetime.now(timezone.utc).isoformat()
            write_json(destination / "metadata.json", metadata)
            bundle = destination.with_suffix(".zip")
            with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as archive:
                for path in destination.rglob("*"):
                    if path.is_file() and path.suffix in {".json", ".csv", ".html", ".md", ".png", ".log"}:
                        archive.write(path, path.relative_to(destination))
    print(json.dumps(summary, indent=2))
    print(f"Report: {destination / 'REPORT.md'}\nEvidence bundle: {bundle}")
    return 0 if summary["target_met"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
