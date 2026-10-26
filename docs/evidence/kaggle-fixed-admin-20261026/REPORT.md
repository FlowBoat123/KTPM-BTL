# Kaggle CPU: one admin, distinct doctors

Source commit: 298d97fcd96d0bb89c617bba782f47d2a4f23584
Same sessions across login/warm-up/measurement. Warm-up 15s AFTER every session is ready; measure 120s plus draining in-flight requests. See phases.json for actual elapsed time. Fresh backend/DB per scenario.

| Customers | Doctors | Admin | Total | Requests | Failures | RPS | Mean ms | P95 ms |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 5 | 1 | 106 | 16280 | 0 | 135.61 | 6.53 | 22 |
| 200 | 10 | 1 | 211 | 32477 | 0 | 270.50 | 10.26 | 32 |
| 500 | 20 | 1 | 521 | 68116 | 0 | 566.49 | 147.20 | 310 |

Customers read/book/look up; 20% of new bookings cancel immediately. One admin reads and assigns pending bookings. Distinct doctors read/confirm/complete their own cases. Think time 1–2.5s per task.

Login/startup metrics are in setup_stats.csv; measurement excludes login/warm-up. Backend and Locust share CPU; not maximum capacity. One admin may leave a growing pending queue. Historical 7/2/1 results are a different workload, not a before/after optimization comparison.