"""One process keeps the same sessions across startup, warm-up and measurement."""
from gevent import monkey
monkey.patch_all()

import argparse
import csv
import json
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import gevent
from locust.env import Environment
from locust.html import get_html_report
from locust.stats import StatsCSV, stats_history

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import locustfile as scenario
from locustfile import AdminUser, CustomerUser, DoctorUser, READY


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--spawn-rate", type=int, required=True)
    parser.add_argument("--warmup", type=int, required=True)
    parser.add_argument("--duration", type=int, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    expected = {c.__name__: c.fixed_count for c in [AdminUser, DoctorUser, CustomerUser]}
    env = Environment(user_classes=[AdminUser, DoctorUser, CustomerUser], host=args.host, stop_timeout=0)
    runner = env.create_local_runner()
    stats = StatsCSV(env, [.5, .9, .95, .99, 1.0])
    phases = {"expected_counts": expected, "stage": "starting"}

    def pause_and_drain():
        scenario.RUN_GATE.clear()
        deadline = time.monotonic() + 30
        while scenario.IN_FLIGHT:
            if time.monotonic() > deadline:
                raise RuntimeError("Requests did not drain at measurement boundary")
            gevent.sleep(.01)

    def export(prefix):
        for suffix, method in [("stats", stats.requests_csv), ("failures", stats.failures_csv), ("exceptions", stats.exceptions_csv)]:
            with (args.output / f"{prefix}_{suffix}.csv").open("w", newline="", encoding="utf-8") as stream:
                method(csv.writer(stream))

    try:
        runner.start(sum(expected.values()), args.spawn_rate)
        deadline = time.monotonic() + 120
        while READY != expected or runner.user_classes_count != expected:
            if time.monotonic() > deadline or env.stats.total.num_failures or runner.exceptions:
                raise RuntimeError(f"Population/login failed: ready={READY}, active={runner.user_classes_count}")
            gevent.sleep(.1)
        phases["ready_counts"] = dict(READY)
        phases["stage"] = "warmup"
        gevent.sleep(args.warmup)
        pause_and_drain()
        export("setup")
        phases["setup_requests"] = env.stats.total.num_requests
        phases["setup_failures"] = env.stats.total.num_failures
        if env.stats.total.num_failures or runner.exceptions:
            raise RuntimeError("Startup/warm-up failed; see setup CSV")
        env.stats.reset_all()
        phases["measurement_start"] = datetime.now(timezone.utc).isoformat()
        phases["stage"] = "measuring"
        history_greenlet = gevent.spawn(stats_history, runner)
        start = time.monotonic()
        scenario.RUN_GATE.set()
        with (args.output / "locust_stats_history.csv").open("w", newline="", encoding="utf-8") as stream:
            writer = csv.writer(stream)
            writer.writerow(["Timestamp", "User Count", "AdminUser", "DoctorUser", "CustomerUser", "Total Request Count", "Total Failure Count", "P95 ms"])
            while time.monotonic() - start < args.duration:
                counts = runner.user_classes_count
                if counts != expected or runner.exceptions:
                    raise RuntimeError(f"Population changed during measurement: {counts}")
                total = env.stats.total
                writer.writerow([time.time(), runner.user_count, counts["AdminUser"], counts["DoctorUser"], counts["CustomerUser"], total.num_requests, total.num_failures, total.get_response_time_percentile(.95) or 0])
                stream.flush()
                gevent.sleep(min(1, max(0, args.duration - (time.monotonic() - start))))
        phases["observed_counts_at_end"] = runner.user_classes_count
        pause_and_drain()
        runner.stop()
        history_greenlet.kill()
        phases["measurement_end"] = datetime.now(timezone.utc).isoformat()
        phases["measurement_elapsed_seconds"] = time.monotonic() - start
        export("locust")
        (args.output / "report.html").write_text(get_html_report(env, show_download_link=False), encoding="utf-8")
        phases["stage"] = "completed"
        return 0
    finally:
        runner.quit()
        (args.output / "phases.json").write_text(json.dumps(phases, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
