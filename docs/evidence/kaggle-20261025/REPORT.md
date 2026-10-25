# SQLite performance baseline

Environment: **kaggle**
Source commit: `8e7b4476e12257292475b1d0aa19ba26ae103ae8`
Users: 50; measurement: 120s; warm-up: 15s.

| Metric | Value |
| --- | --- |
| requests | 6511 |
| failures | 0 |
| failure_ratio | 0.0 |
| rps | 54.56415753650067 |
| mean_ms | 6.786477101213216 |
| p95_ms | 16.0 |
| target_met | True |

Load generator and backend share the same host. JVM heap is fixed at 512 MiB. SQLite uses one application connection and its default journal mode; no Phase 2 tuning is applied.

Task/user weights do not equal proportions of HTTP requests. This workload includes public reads and booking/lookup/cancellation writes plus authenticated Admin/Doctor reads. Assignment and ownership rules are checked before measurement, not benchmarked concurrently.

The failure target is a project choice, not a threshold required by the assignment. A local smoke test is not Kaggle evidence.
