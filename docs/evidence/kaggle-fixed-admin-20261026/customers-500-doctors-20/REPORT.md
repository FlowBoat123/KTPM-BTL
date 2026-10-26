# SQLite performance baseline

Environment: **kaggle**
Source commit: `298d97fcd96d0bb89c617bba782f47d2a4f23584`
Users: 521; measurement: 120s; warm-up: 15s.

| Metric | Value |
| --- | --- |
| requests | 68116 |
| failures | 0 |
| failure_ratio | 0.0 |
| rps | 566.486534099108 |
| mean_ms | 147.19599671268173 |
| p95_ms | 310.0 |
| target_met | True |
| measurement_elapsed_seconds | 120.24292882500004 |

Population: {'AdminUser': 1, 'DoctorUser': 20, 'CustomerUser': 500}. All sessions finish login/spawning and warm-up before statistics reset; the same sessions continue for the measurement. See setup_stats.csv for login/startup metrics and phases.json for exact timing.

Customers read/book/look up and cancel 20% of new bookings. One admin reads and assigns pending requests; distinct doctors read/confirm/complete their own cases. Think time is 1–2.5 seconds per task. A single admin can become a business bottleneck; this is not an unlimited assignment worker test.

Backend and Locust share Kaggle CPU. SQLite pool=1, JVM heap=512 MiB. No database/JAR/token is bundled. The error threshold is a project choice. Historical weighted-user results are a different workload and are not an optimization comparison.
