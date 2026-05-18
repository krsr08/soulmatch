SoulMatch Matrimonial App

A complete matrimonial app with Android + Backend + Admin Panel.
Quick Start

Run the local setup helper, then start the Docker development stack:

```powershell
.\tools\soulmatch-master-setup.ps1
docker compose -f docker/docker-compose.dev.yml up --build
```

Admin: http://localhost:3000

Create local admin credentials in `backend/admin-service/.env` for development. Production requires `ADMIN_PASSWORD_HASH`; plaintext `ADMIN_PASSWORD` is intentionally rejected when `NODE_ENV=production`.

Local OTP testing is available only when `MOCK_OTP=true` in `backend/auth-service/.env`. Keep `MOCK_OTP=false` in production.

Production readiness gates are documented in:

- `docs/RELEASE_CHECKLIST.md`
- `docs/RUNBOOK.md`
- `docs/OBSERVABILITY.md`
- `docs/SECURITY_TESTS.md`

Developer architecture references:

- `docs/ARCHITECTURE_IMPLEMENTATION_CHECKLIST.md`
- `docs/ARCHITECTURE_FOUNDATION.md`
- `docs/DEVELOPER_BOUNDARIES.md`
- `docs/EXTENSION_POINTS.md`
- `docs/project-structure.md`
- `docs/MODULE_OWNERSHIP_MAP.md`
- `docs/RUNTIME_CONFIG_CONTROL_PLANE.md`
- `docs/PRODUCTION_SMOKE_STATUS.md`
- `docs/DEVELOPMENT_WORKFLOW.md`

Pull requests run `.github/workflows/ci.yml`, which builds the Docker dev stack, applies migrations, runs smoke tests, audits dependencies, scans secrets, and runs Android unit tests.
