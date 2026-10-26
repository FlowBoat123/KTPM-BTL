# SQLite performance baseline

Environment: **kaggle**
Source commit: `298d97fcd96d0bb89c617bba782f47d2a4f23584`
Users: 211; measurement: 120s; warm-up: 15s.

| Metric | Value |
| --- | --- |
| requests | 32477 |
| failures | 0 |
| failure_ratio | 0.0 |
| rps | 270.5010202833227 |
| mean_ms | 10.261860780706572 |
| p95_ms | 32.0 |
| target_met | True |
| measurement_elapsed_seconds | 120.062393724 |

Population: {'AdminUser': 1, 'DoctorUser': 10, 'CustomerUser': 200}. All sessions finish login/spawning and warm-up before statistics reset; the same sessions continue for the measurement. See setup_stats.csv for login/startup metrics and phases.json for exact timing.

Customers read/book/look up and cancel 20% of new bookings. One admin reads and assigns pending requests; distinct doctors read/confirm/complete their own cases. Think time is 1–2.5 seconds per task. A single admin can become a business bottleneck; this is not an unlimited assignment worker test.

Backend and Locust share Kaggle CPU. SQLite pool=1, JVM heap=512 MiB. No database/JAR/token is bundled. The error threshold is a project choice. Historical weighted-user results are a different workload and are not an optimization comparison.
