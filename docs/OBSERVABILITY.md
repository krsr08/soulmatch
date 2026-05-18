# SoulMatch Observability

## Metrics

All backend services expose Prometheus metrics at `/metrics`:

- `soulmatch_http_requests_total{service,method,route,status,status_class}`
- `soulmatch_http_request_duration_seconds_bucket`
- `soulmatch_http_request_duration_seconds_sum`
- `soulmatch_http_request_duration_seconds_count`
- `soulmatch_process_uptime_seconds{service}`
- `soulmatch_notification_outbox_depth`
- `soulmatch_notification_dlq_depth`
- `soulmatch_admin_live_users`
- `soulmatch_admin_pending_approvals`
- `soulmatch_admin_payments_today`
- `soulmatch_admin_pending_reports`

Prometheus config: `docker/prometheus.yml`.
Alert rules: `docker/prometheus-alert-rules.yml`.

## Logs

Each service writes structured request logs to stdout:

```json
{"level":"info","type":"http_request","service":"profile-service","requestId":"...","method":"GET","route":"/api/v1/profile/me","status":200,"durationMs":24,"ip":"10.0.0.4"}
```

Production log collection should retain at least 30 days of application logs and 90 days of security/audit logs.

## Grafana Panels

Create these panels against Prometheus:

| Panel | Query |
| --- | --- |
| Request rate by service | `sum by (service) (rate(soulmatch_http_requests_total[5m]))` |
| 5xx rate | `sum(rate(soulmatch_http_requests_total{status_class="5xx"}[5m])) / clamp_min(sum(rate(soulmatch_http_requests_total[5m])), 1)` |
| p95 latency | `histogram_quantile(0.95, sum by (le, service) (rate(soulmatch_http_request_duration_seconds_bucket[5m])))` |
| OTP failures | `sum(rate(soulmatch_http_requests_total{service="auth-service",route=~".*otp.*",status_class=~"4xx|5xx"}[5m]))` |
| Payment webhook failures | `sum(rate(soulmatch_http_requests_total{service="payment-service",route=~".*webhook.*",status_class=~"4xx|5xx"}[5m]))` |
| Notification queue depth | `soulmatch_notification_outbox_depth` |
| Notification DLQ | `soulmatch_notification_dlq_depth` |
| Admin live users | `soulmatch_admin_live_users` |

## Alerts

Minimum production alerts:

- Error rate above 1% for 5 minutes.
- Any payment webhook failure for 5 minutes.
- Any notification DLQ item for 15 minutes.
- Any service scrape target down for 2 minutes.
- Postgres, MongoDB, or Redis health check failure.
- Disk usage above 80% and 90%.

## Request IDs

Clients may send `x-request-id`. If absent, services generate one and echo it back in the response. Use this ID to correlate gateway, app, and admin audit logs.
