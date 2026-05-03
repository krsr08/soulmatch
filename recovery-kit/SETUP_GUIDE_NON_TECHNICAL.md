# SoulMatch Setup Guide in Simple Language

Use this when you change laptop, lose the VM, or need to recreate the app from zero.

## Part A: New Laptop Setup

1. Install Git for Windows.
2. Install Android Studio.
3. Install Docker Desktop.
4. Install Node.js LTS.
5. Install Azure CLI.
6. Restart the laptop.

Clone the code:

```powershell
cd C:\Users\ANIRUDH\Documents
git clone https://github.com/krsr08/soulmatch.git
cd soulmatch
```

Open Android Studio:

1. Click `Open`.
2. Select `C:\Users\ANIRUDH\Documents\soulmatch\android`.
3. Wait for Gradle sync.
4. Connect phone with USB debugging.
5. Click `Run`.

Command-line Android build:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch\android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Install on connected phone:

```powershell
.\gradlew.bat :app:installDebug --no-daemon
```

## Part B: Recreate Azure VM

Login:

```powershell
az login --tenant b8f1f498-6a72-4560-b038-735e5e0fcc12
```

Create resource group:

```powershell
az group create --name soulmatch-rg --location centralindia
```

Create VM:

```powershell
az vm create `
  --resource-group soulmatch-rg `
  --name soulmatch-vm `
  --image Ubuntu2204 `
  --size Standard_B2ls_v2 `
  --admin-username azureuser `
  --ssh-key-values "$env:USERPROFILE\.ssh\id_rsa.pub" `
  --public-ip-sku Standard
```

Open ports:

```powershell
az network nsg rule create --resource-group soulmatch-rg --nsg-name soulmatch-vmNSG --name allow-http-80 --priority 901 --destination-port-ranges 80 --access Allow --protocol Tcp
az network nsg rule create --resource-group soulmatch-rg --nsg-name soulmatch-vmNSG --name allow-https-443 --priority 902 --destination-port-ranges 443 --access Allow --protocol Tcp
```

SSH:

```powershell
ssh azureuser@NEW_PUBLIC_IP
```

Install Docker and Nginx on VM:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg git nginx
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker azureuser
newgrp docker
```

Clone app:

```bash
cd /home/azureuser
git clone https://github.com/krsr08/soulmatch.git
cd soulmatch
```

Create production env:

```bash
cp docker/production.env.example docker/production.env
nano docker/production.env
```

Paste real secrets from your password manager or GitHub secrets. Save Nano:

1. Press `Ctrl + O`
2. Press `Enter`
3. Press `Ctrl + X`

Deploy:

```bash
bash tools/deploy-production.sh
```

## Part C: Firebase Setup

1. Open Firebase Console.
2. Select `soul-match-2ead9`.
3. Project settings -> Android app.
4. Confirm package name is `com.soulmatch.app`.
5. Add SHA-1 and SHA-256 from this recovery kit.
6. Download `google-services.json`.
7. Put the file in `android/app/google-services.json`.
8. Commit only if this project policy allows it and it contains no OAuth secrets. Otherwise keep it local.

## Part D: Razorpay Setup

1. Open Razorpay Dashboard.
2. Use Test Mode during internal QA.
3. Copy test key ID and secret into VM production env.
4. Add webhook only after HTTPS domain is ready.
5. Test with Razorpay test cards.

## Part E: Daily Safety Habit

Every day before changing code:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
git status
git pull
```

After changing code:

```powershell
git status
git add .
git commit -m "Describe the change"
git push
```

After deployment:

```powershell
ssh -i $env:USERPROFILE\.ssh\soulmatch_github_deploy azureuser@20.204.142.19 "cd /home/azureuser/soulmatch && cat .soulmatch-deployed-version.json"
```

