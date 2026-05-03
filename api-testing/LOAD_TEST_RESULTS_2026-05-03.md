# SoulMatch Production Load Test Results - 2026-05-03

Target: `http://20.204.142.19`

Scope: safe read-only production smoke load test. OTP, payment capture, admin, profile writes, chat writes, and notification writes were not load tested on production.

Tool: `autocannon v8.0.0`

## Results

| Endpoint | Concurrency | Duration | Requests | Avg RPS | Avg Latency | P90 | P97.5 | P99 | Errors |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `/api/v1/public/config` | 5 | 10s | 1,315 | 131.50 | 67.38 ms | 103 ms | 120 ms | 146 ms | 0 |
| `/api/v1/public/config` | 10 | 10s | 2,467 | 246.70 | 72.44 ms | 108 ms | 130 ms | 366 ms | 0 |
| `/api/v1/public/config` | 20 | 10s | 4,949 | 494.90 | 69.16 ms | 108 ms | 127 ms | 184 ms | 0 |
| `/api/v1/public/config` | 40 | 10s | 5,667 | 566.71 | 82.41 ms | 91 ms | 512 ms | 954 ms | 0 |
| `/api/v1/public/config` | 5 | 30s | 3,954 | 131.81 | 168.58 ms | 263 ms | 344 ms | 565 ms | 0 |
| `/api/v1/public/config` | 10 | 30s | 7,020 | 234.00 | 156.56 ms | 244 ms | 383 ms | 732 ms | 0 |
| `/api/v1/public/config` | 20 | 30s | 13,733 | 457.77 | 161.88 ms | 250 ms | 505 ms | 940 ms | 0 |
| `/api/v1/payment/plans` | 10 | 10s | 3,260 | 326.00 | 63.46 ms | 96 ms | 120 ms | 132 ms | 0 |
| `/api/v1/payment/plans` | 20 | 10s | 5,342 | 534.21 | 62.89 ms | 89 ms | 100 ms | 135 ms | 0 |
| `/api/v1/payment/plans` | 40 | 10s | 10,705 | 1,070.50 | 71.24 ms | 106 ms | 134 ms | 352 ms | 0 |
| `/api/v1/payment/upgrade-packages` | 20 | 10s | 4,881 | 488.10 | 65.19 ms | 90 ms | 106 ms | 508 ms | 0 |
| `/api/v1/payment/upgrade-packages` | 40 | 10s | 9,055 | 905.50 | 67.99 ms | 100 ms | 129 ms | 153 ms | 0 |

## Current Capacity Conclusion

The current single Azure VM can handle simple public read APIs at 100-200 RPS comfortably and can burst much higher on short tests without errors.

For production planning, use conservative limits because real app usage includes heavier authenticated profile, search, match, chat, notification, and payment flows:

- Comfortable current launch load: 20-40 simultaneous active app users.
- Read-only API comfort zone: about 100-200 RPS.
- Short read-only burst tolerated: 500-900+ RPS on the tested simple endpoints.
- Do not assume the same capacity for search, matches, chat, OTP, payment, or profile update APIs until authenticated staging load tests are run.

## VM Health After Test

The VM remained healthy after testing:

- No load-test errors, timeouts, resets, or non-2xx responses.
- Around 2.4 GB RAM available after the tests.
- Docker containers remained running.

## Next Load Test Needed

Create a staging test account and run authenticated flow tests:

- Login token reuse test.
- Best matches read.
- Search read.
- Profile detail read.
- Interest send/accept/decline on staging only.
- Chat send/read on staging only.
- Payment order creation on test/staging only.

Production write endpoints should not be load tested directly.
