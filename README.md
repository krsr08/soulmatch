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
