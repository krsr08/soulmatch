# SoulMatch Deployment Notes

## Production Network Boundary

Production services must not expose databases or service containers directly to the internet.

- Public ingress: a single reverse proxy on ports `80` / `443`.
- Internal services: `auth-service`, `profile-service`, `matching-service`, `search-service`, `chat-service`, `notification-service`, `payment-service`, `admin-service`, and `admin-web`.
- Data services: Postgres, MongoDB, and Redis stay internal-only and must not publish host ports.

The current VM rollout uses host Nginx configured by `tools/deploy-production.sh` as the public gateway. Docker Compose binds backend service ports to `127.0.0.1` only so they are reachable by the VM-local gateway and health checks but not by remote clients.

## TLS

Before launch, terminate HTTPS at Nginx, a managed load balancer, or a WAF/API gateway. Redirect HTTP to HTTPS after certificates are installed.

Required production environment values:

```env
CORS_ORIGINS=https://soulmatch.app,https://admin.soulmatch.app
ADMIN_API_PUBLIC_URL=https://soulmatch.app/api/v1/admin
```

Android release builds should use one public API base URL:

```properties
AUTH_BASE_URL=https://soulmatch.app/api/v1/
PROFILE_BASE_URL=https://soulmatch.app/api/v1/
MATCHING_BASE_URL=https://soulmatch.app/api/v1/
SEARCH_BASE_URL=https://soulmatch.app/api/v1/
CHAT_BASE_URL=https://soulmatch.app/api/v1/
NOTIFICATION_BASE_URL=https://soulmatch.app/api/v1/
PAYMENT_BASE_URL=https://soulmatch.app/api/v1/
CONTROL_PLANE_BASE_URL=https://soulmatch.app/api/v1/
```

## OTP Rate Limits

Production OTP controls:

- `OTP_PHONE_LIMIT=3` per 15 minutes.
- `OTP_IP_LIMIT=10` per 15 minutes.
- `MOCK_OTP=false`; production boot fails if mock OTP is enabled.

## Gateway Routing

The production gateway must route only these public paths:

- `/api/v1/auth/`
- `/api/v1/profile/`
- `/api/v1/matches/`
- `/api/v1/interests/`
- `/api/v1/matching/`
- `/api/v1/search/`
- `/api/v1/chat/`
- `/socket.io/`
- `/api/v1/notifications/`
- `/api/v1/payment/`
- `/api/v1/admin/`
- `/api/v1/public/`
- `/uploads/` only when local storage is intentionally enabled.

## Production Database Pooling

Before paid launch, put PgBouncer in front of Postgres or use a managed Postgres pooler.

Recommended starting point:

- Pool mode: transaction.
- Default pool size: 20.
- Reserve pool size: 5.
- Max client connections: 200.
- Each service uses PgBouncer as `DB_HOST`.

Keep direct Postgres access available only for migrations and emergency admin tasks.

## Invoices, GST, and Razorpay

SoulMatch stores payment orders, transactions, and subscriptions locally and reconciles Razorpay webhooks idempotently. Production invoicing options:

- Use Razorpay invoices for legally formatted receipts when GST registration is active.
- Store generated invoice number, Razorpay invoice ID, transaction ID, amount, tax, plan, validity, and issued timestamp against the subscription.
- Keep the current app invoice view as a stub until GST/business details are finalized.

## Production Seeding

`docker/docker-compose.prod.yml` must never mount demo seed files. Demo/sample profiles are allowed only in local development or staging with explicit seed scripts.

## Release Audit

Every production deployment should write a release audit entry with:

- Commit SHA and release version.
- Timestamp and operator.
- Release description and changed areas.
- Deployment result.
- Migration list.
