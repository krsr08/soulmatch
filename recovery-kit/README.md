# SoulMatch Recovery Kit

This folder is the rebuild map for SoulMatch. Keep it with the code and also keep a copy outside the repo at:

`C:\Users\ANIRUDH\Documents\soulmatch-recovery-kit`

Purpose:

1. Rebuild the Android app from a new laptop.
2. Recreate the Azure VM deployment if the trial cloud is lost.
3. Reconnect GitHub Actions, Firebase, Google Sign-In, and Razorpay.
4. Verify that local code, cloud code, and production deployment are in sync.

Important security rule:

Do not commit real secrets here. Use placeholders in templates, then paste real values only into:

1. Azure VM file: `/home/azureuser/soulmatch/docker/production.env`
2. GitHub repository secrets: `Settings -> Secrets and variables -> Actions`
3. Local untracked files such as `android/local.properties`

Known non-secret configuration:

| Area | Value |
| --- | --- |
| GitHub repo | `https://github.com/krsr08/soulmatch.git` |
| Main branch | `main` |
| Azure resource group | `soulmatch-rg` |
| Azure VM | `soulmatch-vm` |
| Azure region | `centralindia` |
| Azure public IP | `20.204.142.19` |
| VM user | `azureuser` |
| Android package | `com.soulmatch.app` |
| Firebase project ID | `soul-match-2ead9` |
| Firebase project number | `253330028301` |
| Google Web client ID | `253330028301-46qp0puk1rj2nvpmoklagv4njcta6do7.apps.googleusercontent.com` |
| Distribution SHA-1 | `8D:EB:DA:4B:81:7A:BD:60:EC:B0:23:E3:E4:00:B1:2E:80:11:3B:B4` |
| Distribution SHA-256 | `CC:81:52:34:FE:71:F1:FE:A4:BC:DB:F2:B0:2E:57:2F:4F:5A:0F:84:BD:97:5A:C5:6C:D8:D4:A7:91:61:AA:B8` |

Read in this order:

1. `SETUP_GUIDE_NON_TECHNICAL.md`
2. `CURRENT_CONFIGURATION_MAP.md`
3. `FIVE_DAY_PRODUCTION_PLAN.md`
4. `scripts\one-click-local-powerup.ps1`

API testing assets:

- Main repo: `C:\Users\ANIRUDH\Documents\soulmatch\api-testing`
- Backup copy: `C:\Users\ANIRUDH\Documents\soulmatch-recovery-kit\api-testing`
- Import `soulmatch.postman_collection.json` and `soulmatch.postman_environment.json` into Postman.
- Run safe load tests with `k6\soulmatch-smoke-load.js`.
