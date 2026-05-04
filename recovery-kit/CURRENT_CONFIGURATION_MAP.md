# Current Configuration Map

This document explains where every important SoulMatch setting lives.

## 1. Local Laptop

Folder:

`C:\Users\ANIRUDH\Documents\soulmatch`

Main folders:

| Folder | Purpose |
| --- | --- |
| `android` | Android member app source code |
| `admin-web` | Admin web dashboard |
| `backend` | All backend services |
| `database` | Schema, migrations, and seed data |
| `docker` | Local and production Compose files |
| `tools` | Deploy, backup, restore, monitor scripts |
| `docs` | Architecture, runbooks, handover notes |
| `recovery-kit` | Simple rebuild guide and setup scripts |

Local files that must exist but must not be committed:

| File | Why |
| --- | --- |
| `android/local.properties` | Android SDK path for this laptop |
| `docker/production.env` | Production secrets for Azure VM |
| `*.jks`, `*.keystore` | Android signing keys |
| `client_secret*.json` | Google OAuth secret files |
| `*.apk` | Built app packages |

## 2. Azure VM

Azure Portal navigation:

1. Open [Azure Portal](https://portal.azure.com).
2. Search `Resource groups`.
3. Open `soulmatch-rg`.
4. Open `soulmatch-vm`.
5. Confirm public IP is `20.204.142.19`.

SSH from Windows PowerShell:

```powershell
ssh -i $env:USERPROFILE\.ssh\soulmatch_github_deploy azureuser@20.204.142.19
```

Production app folder on VM:

```bash
/home/azureuser/soulmatch
```

Production environment file on VM:

```bash
/home/azureuser/soulmatch/docker/production.env
```

Do not put this file in GitHub.

Deploy command on VM:

```bash
cd /home/azureuser/soulmatch
bash tools/deploy-production.sh
```

Health checks:

```bash
curl http://127.0.0.1:3001/health
curl http://127.0.0.1:3002/health
curl http://127.0.0.1:3003/health
curl http://127.0.0.1:3004/health
curl http://127.0.0.1:3005/health
curl http://127.0.0.1:3006/health
curl http://127.0.0.1:3007/health
curl http://127.0.0.1:3011/health
curl http://127.0.0.1/api/v1/public/config
```

Home page merchandising config:

1. Open Admin Web.
2. Go to `CMS`.
3. Open `Mobile App Content` or `Page-wise Static Content`.
4. Edit `content.home`.

Important Home keys:

| Key | Purpose |
| --- | --- |
| `bestMatchMinimumProfiles` | Minimum real profile cards shown in Best Matches. Keep `5` or higher. |
| `bestMatchInsertFrequency` | How often to insert upgrade/ad cards between profiles. `2` means after every two profile cards. |
| `showBestMatchInsertCards` | Master ON/OFF for all insert cards. |
| `showBestMatchUpgradeCards` | ON/OFF for membership upgrade cards. The Android app still hides them for highest-plan users. |
| `showBestMatchAdCards` | ON/OFF for ad cards. |
| `bestMatchAdCards` | Product, marriage, astrology, or profile ad cards shown between profile cards. |

## 3. GitHub

Repository:

`https://github.com/krsr08/soulmatch.git`

GitHub Actions navigation:

1. Open GitHub repo.
2. Click `Actions`.
3. Open `SoulMatch Production Deploy`.
4. Check the latest workflow run.

Secrets navigation:

1. Open GitHub repo.
2. Click `Settings`.
3. Click `Secrets and variables`.
4. Click `Actions`.
5. Add or update secrets from `templates/github-actions-secrets.md`.

## 4. Firebase

Firebase Console navigation:

1. Open [Firebase Console](https://console.firebase.google.com).
2. Select project `soul-match-2ead9`.
3. Open `Project settings`.
4. Open Android app package `com.soulmatch.app`.
5. Confirm SHA-1 and SHA-256 match this kit.
6. Download `google-services.json`.
7. Put it at `android/app/google-services.json`.

Phone OTP navigation:

1. Firebase Console -> `Authentication`.
2. `Sign-in method`.
3. Enable `Phone`.
4. Add test phone numbers only for internal QA.
5. Do not leave public mock OTP enabled in production.

## 5. Google Cloud OAuth

Google Cloud Console navigation:

1. Open [Google Cloud Console](https://console.cloud.google.com).
2. Select project `soul-match-2ead9`.
3. Go to `APIs and services`.
4. Open `Credentials`.
5. Confirm there is:
   - Android OAuth client for `com.soulmatch.app`
   - Web OAuth client used by backend and Android config

Expected Web client ID:

`253330028301-46qp0puk1rj2nvpmoklagv4njcta6do7.apps.googleusercontent.com`

## 6. Razorpay

Razorpay Dashboard navigation:

1. Open Razorpay Dashboard.
2. Switch to `Test Mode` for testing.
3. Go to `Account and Settings`.
4. Open `API Keys`.
5. Copy `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` into VM `docker/production.env`.
6. Go to `Webhooks`.
7. Add webhook URL after domain/HTTPS is ready:

```text
https://YOUR_DOMAIN/api/v1/payment/webhook
```

Until a domain is added, Android payment can still confirm payment through the app verify call, but Razorpay webhook delivery needs a public HTTPS endpoint.
