# SoulMatch Automated Deployment

This repo is configured for automatic production deployment through GitHub Actions.

## What Happens on Every Push

Pushing to `main` or `master` runs `.github/workflows/soulmatch-production-deploy.yml`.

1. Installs backend Node dependencies for every service.
2. Installs and bytecode-validates the Python matching service.
3. Builds the admin web app.
4. Builds Android debug and release APKs.
5. Runs Android unit tests.
6. Uploads the debug APK and unsigned release APK as workflow artifacts.
7. Distributes the debug APK through Firebase App Distribution when Firebase secrets are configured.
8. Syncs the source code to the Azure VM.
9. Runs `tools/deploy-production.sh` on the VM.
10. Rebuilds Docker images, starts containers, applies pending DB migrations, and health-checks every service.

## Required GitHub Secrets

Add these in GitHub:

- `AZURE_VM_HOST`: VM public IP or domain, for example `20.204.142.19`.
- `AZURE_VM_USER`: SSH user, for example `azureuser`.
- `AZURE_VM_SSH_KEY`: private SSH key with access to the VM.
- `AZURE_VM_PORT`: optional, defaults to `22`.
- `AZURE_DEPLOY_PATH`: optional, defaults to `/home/azureuser/soulmatch`.

Do not store production secrets in GitHub unless you later move to a managed secret store. Keep the live `docker/production.env` only on the VM for this setup.

## Firebase App Distribution Secrets

The workflow distributes `android/app/build/outputs/apk/debug/app-debug.apk` to testers after the Android build succeeds.

Add these repository secrets:

- `FIREBASE_PROJECT_ID`: `soul-match-2ead9`
- `FIREBASE_APP_ID_ANDROID`: `1:253330028301:android:4a4647b92b64d0ebec6244`
- `FIREBASE_SERVICE_ACCOUNT_JSON`: full JSON key for a service account with Firebase App Distribution permission.
- `FIREBASE_DISTRIBUTION_GROUPS`: tester group aliases, for example `testers`.
- `FIREBASE_TESTERS`: optional comma-separated tester emails, for example `qa1@example.com,qa2@example.com`.
- `APP_PUBLIC_API_BASE_URL`: optional, defaults to `http://20.204.142.19/api/v1/`.
- `RAZORPAY_KEY_ID`: optional Android test checkout key.
- `GOOGLE_WEB_CLIENT_ID`: optional Google Sign-In web client ID.

Set at least one of `FIREBASE_DISTRIBUTION_GROUPS` or `FIREBASE_TESTERS`. If the Firebase service account secret is missing, the workflow skips App Distribution but still builds the APK artifact.

## VM Requirements

The Azure VM must already have:

- Docker and Docker Compose installed.
- Nginx configured to proxy public traffic to the local service ports.
- `/home/azureuser/soulmatch/docker/production.env` present with real runtime values.
- Port 22 open to GitHub Actions runners, or to your own self-hosted runner.

## Deployment Script

`tools/deploy-production.sh` is safe to run manually on the VM:

```bash
cd /home/azureuser/soulmatch
bash tools/deploy-production.sh
```

It validates compose config, rebuilds services, starts containers, applies pending SQL migrations using `schema_migrations`, checks health endpoints, and prunes Docker build cache.

## Rollback

For now, rollback is done by reverting the bad commit and pushing the revert to `main` or `master`. The workflow will redeploy the last good source state automatically.

Before high-traffic production, add versioned Docker image publishing, Azure Container Registry, release tags, and one-click rollback to a previous image tag.
