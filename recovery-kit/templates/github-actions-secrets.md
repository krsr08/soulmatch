# GitHub Actions Secrets

Navigation:

1. GitHub repo -> `Settings`
2. `Secrets and variables`
3. `Actions`
4. `New repository secret`

Add these names. Paste real values from your machine, Azure, Firebase, and Razorpay.

| Secret name | Value source |
| --- | --- |
| `AZURE_VM_HOST` | `20.204.142.19` |
| `AZURE_VM_USER` | `azureuser` |
| `AZURE_VM_SSH_KEY` | Private key from `C:\Users\ANIRUDH\.ssh\soulmatch_github_deploy` |
| `FIREBASE_APP_ID` | Firebase Android app ID |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase service account JSON with App Distribution permissions |
| `GOOGLE_WEB_CLIENT_ID` | `253330028301-46qp0puk1rj2nvpmoklagv4njcta6do7.apps.googleusercontent.com` |
| `ANDROID_KEYSTORE_BASE64` | Base64 of release/distribution `.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Android keystore password |
| `ANDROID_KEY_ALIAS` | Android key alias |
| `ANDROID_KEY_PASSWORD` | Android key password |

Security notes:

1. Do not paste secrets into chat, docs, or git commits.
2. Rotate a secret immediately if it is accidentally exposed.
3. Keep the GitHub repo private while the app is under active development.

