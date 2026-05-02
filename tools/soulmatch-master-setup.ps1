param(
    [ValidateSet("Help", "Local", "VM", "GitHub", "Status", "OfflineBackup", "All")]
    [string]$Mode = "Help",

    [string]$RepoUrl = "https://github.com/krsr08/soulmatch.git",
    [string]$GitHubRepo = "krsr08/soulmatch",
    [string]$GitHubSecretsFile = ".\soulmatch-github-secrets.local.json",

    [string]$VmHost = "20.204.142.19",
    [string]$VmUser = "azureuser",
    [string]$VmPort = "22",
    [string]$SshKeyPath = "$env:USERPROFILE\.ssh\soulmatch_github_deploy",
    [string]$DeployPath = "/home/azureuser/soulmatch",

    [string]$ApiBaseUrl = "",
    [string]$LocalBackupDestination = "$env:USERPROFILE\Documents\soulmatch-backups",

    [switch]$UploadCurrentRepo,
    [switch]$RunBuild
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Write-Host "[$timestamp] $Message"
}

function Write-Warn {
    param([string]$Message)
    Write-Host "WARNING: $Message" -ForegroundColor Yellow
}

function Require-Command {
    param([string]$Name, [switch]$Required)
    $found = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $found -and $Required) {
        throw "Missing required command: $Name"
    }
    if ($found) {
        Write-Step "$Name found"
        return $true
    }
    Write-Warn "$Name not found"
    return $false
}

function Invoke-Remote {
    param([string]$Command)
    Require-Command "ssh" -Required | Out-Null
    if (-not (Test-Path -LiteralPath $SshKeyPath)) {
        throw "SSH key not found: $SshKeyPath"
    }
    ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" $Command
}

function Show-Help {
    Write-Host @"
SoulMatch Master Setup

Use this one script for common setup and operations.

Examples:

1. Prepare this Windows laptop:
   powershell -ExecutionPolicy Bypass -File .\tools\soulmatch-master-setup.ps1 -Mode Local -VmHost 20.204.142.19

2. Prepare or power up the VM:
   powershell -ExecutionPolicy Bypass -File .\tools\soulmatch-master-setup.ps1 -Mode VM -VmHost 20.204.142.19 -UploadCurrentRepo

3. Check production health:
   powershell -ExecutionPolicy Bypass -File .\tools\soulmatch-master-setup.ps1 -Mode Status -VmHost 20.204.142.19

4. Download offline backups to laptop:
   powershell -ExecutionPolicy Bypass -File .\tools\soulmatch-master-setup.ps1 -Mode OfflineBackup -LocalBackupDestination "$env:USERPROFILE\Documents\soulmatch-backups"

5. Prepare GitHub secrets from a local JSON file:
   powershell -ExecutionPolicy Bypass -File .\tools\soulmatch-master-setup.ps1 -Mode GitHub -GitHubSecretsFile .\soulmatch-github-secrets.local.json

Real secrets are never generated into Git. Keep local secret files outside Git.
"@
}

function Setup-Local {
    Write-Step "Checking local laptop tools"
    Require-Command "git" | Out-Null
    Require-Command "ssh" | Out-Null
    Require-Command "scp" | Out-Null
    Require-Command "az" | Out-Null
    Require-Command "docker" | Out-Null
    Require-Command "node" | Out-Null
    Require-Command "npm" | Out-Null
    Require-Command "java" | Out-Null

    if (-not $ApiBaseUrl) {
        $ApiBaseUrl = "http://$VmHost/api/v1/"
    }
    if (-not $ApiBaseUrl.EndsWith("/")) {
        $ApiBaseUrl = "$ApiBaseUrl/"
    }

    $androidLocal = Join-Path $PWD "android\local.properties"
    $androidExample = Join-Path $PWD "android\local.properties.example"
    if (-not (Test-Path -LiteralPath $androidLocal)) {
        if (-not (Test-Path -LiteralPath $androidExample)) {
            throw "Missing android/local.properties.example"
        }

        $sdk = "$env:LOCALAPPDATA\Android\Sdk"
        $content = Get-Content -Raw -Path $androidExample
        if (Test-Path -LiteralPath $sdk) {
            $escapedSdk = $sdk.Replace("\", "\\")
            $content = $content -replace "sdk.dir=.*", "sdk.dir=$escapedSdk"
        }

        $content = $content -replace "AUTH_BASE_URL=.*", "AUTH_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "PROFILE_BASE_URL=.*", "PROFILE_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "MATCHING_BASE_URL=.*", "MATCHING_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "SEARCH_BASE_URL=.*", "SEARCH_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "CHAT_BASE_URL=.*", "CHAT_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "PAYMENT_BASE_URL=.*", "PAYMENT_BASE_URL=$ApiBaseUrl"
        $content = $content -replace "CONTROL_PLANE_BASE_URL=.*", "CONTROL_PLANE_BASE_URL=$ApiBaseUrl"
        Set-Content -Path $androidLocal -Value $content -Encoding ascii
        Write-Step "Created android/local.properties"
    } else {
        Write-Step "android/local.properties already exists"
    }

    if ($RunBuild) {
        Write-Step "Running Android debug build"
        Push-Location android
        try {
            .\gradlew.bat :app:assembleDebug --no-daemon
        } finally {
            Pop-Location
        }
    }

    Write-Step "Local setup check complete"
}

function Setup-VM {
    Write-Step "Preparing VM $VmUser@$VmHost"

    $remotePrep = @"
set -euo pipefail
sudo apt-get update
sudo apt-get install -y git curl ca-certificates gnupg nginx rsync
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker `$(whoami) || true
fi
if ! command -v az >/dev/null 2>&1; then
  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
fi
mkdir -p '$DeployPath'
echo 'VM prerequisites installed.'
"@
    Invoke-Remote $remotePrep

    if ($UploadCurrentRepo) {
        Require-Command "git" -Required | Out-Null
        Require-Command "scp" -Required | Out-Null
        $archive = Join-Path $env:TEMP ("soulmatch-" + [guid]::NewGuid().ToString("N") + ".tar")
        Write-Step "Creating clean Git archive for upload"
        git archive --format=tar --output $archive HEAD
        Write-Step "Uploading archive to VM"
        scp -i $SshKeyPath -P $VmPort $archive "$VmUser@$VmHost`:/tmp/soulmatch-source.tar"
        Remove-Item -LiteralPath $archive -Force
        Invoke-Remote "mkdir -p '$DeployPath' && tar -xf /tmp/soulmatch-source.tar -C '$DeployPath' && rm -f /tmp/soulmatch-source.tar"
    }

    $remoteDeploy = @"
set -euo pipefail
cd '$DeployPath'
if [ ! -f docker/production.env ]; then
  cp docker/production.env.example docker/production.env
  echo 'Created docker/production.env from example.'
  echo 'Edit this file on the VM and add real secrets before production deploy.'
  exit 20
fi
for script in tools/*.sh; do
  [ -f "`$script" ] && sed -i 's/\r$//' "`$script" && chmod +x "`$script"
done
bash tools/deploy-production.sh
bash tools/install-backup-cron.sh || true
bash tools/install-monitor-cron.sh || true
bash tools/monitor-production-health.sh || true
"@
    try {
        Invoke-Remote $remoteDeploy
    } catch {
        if ($_.Exception.Message -match "20") {
            Write-Warn "VM production env was created from template. Fill real values, then rerun Mode VM."
        } else {
            throw
        }
    }
}

function Setup-GitHub {
    if (-not (Test-Path -LiteralPath $GitHubSecretsFile)) {
        Write-Step "Creating local GitHub secrets template: $GitHubSecretsFile"
        $template = [ordered]@{
            AZURE_VM_HOST = $VmHost
            AZURE_VM_USER = $VmUser
            AZURE_VM_PORT = $VmPort
            AZURE_DEPLOY_PATH = $DeployPath
            AZURE_VM_SSH_KEY = "PASTE_PRIVATE_DEPLOY_SSH_KEY_HERE"
            APP_PUBLIC_API_BASE_URL = "http://$VmHost/api/v1/"
            FIREBASE_PROJECT_ID = "soul-match-2ead9"
            FIREBASE_APP_ID_ANDROID = "1:253330028301:android:4a4647b92b64d0ebec6244"
            FIREBASE_SERVICE_ACCOUNT_JSON = "PASTE_FIREBASE_SERVICE_ACCOUNT_JSON_HERE"
            FIREBASE_DISTRIBUTION_GROUPS = "testers"
            FIREBASE_TESTERS = ""
            RAZORPAY_KEY_ID = "PASTE_RAZORPAY_KEY_ID_HERE"
            GOOGLE_WEB_CLIENT_ID = "253330028301-46qp0puk1rj2nvpmoklagv4njcta6do7.apps.googleusercontent.com"
            ANDROID_DISTRIBUTION_KEYSTORE_BASE64 = "PASTE_BASE64_JKS_HERE"
            ANDROID_DISTRIBUTION_KEYSTORE_PASSWORD = "PASTE_KEYSTORE_PASSWORD_HERE"
            ANDROID_DISTRIBUTION_KEY_ALIAS = "soulmatch-distribution"
            ANDROID_DISTRIBUTION_KEY_PASSWORD = "PASTE_KEY_PASSWORD_HERE"
        }
        $template | ConvertTo-Json | Set-Content -Path $GitHubSecretsFile -Encoding utf8
        Write-Warn "Fill $GitHubSecretsFile with real values, then rerun Mode GitHub."
        return
    }

    $hasGh = Require-Command "gh"
    if (-not $hasGh) {
        Write-Warn "GitHub CLI is not installed. Use the values in $GitHubSecretsFile and add them manually in GitHub > Settings > Secrets and variables > Actions."
        return
    }

    $json = Get-Content -Raw -Path $GitHubSecretsFile | ConvertFrom-Json
    $props = $json.PSObject.Properties
    foreach ($prop in $props) {
        $name = $prop.Name
        $value = [string]$prop.Value
        if (-not $value -or $value -match "^PASTE_") {
            Write-Warn "Skipping $name because it has no real value yet"
            continue
        }
        Write-Step "Setting GitHub secret $name"
        $value | gh secret set $name --repo $GitHubRepo --body-file -
    }

    Write-Step "GitHub secret setup complete"
}

function Show-Status {
    Write-Step "Checking production status on VM"
    $remoteStatus = @"
set -euo pipefail
echo '--- docker services ---'
cd '$DeployPath'
docker compose -f docker/docker-compose.prod.yml ps
echo '--- monitor report ---'
cat /home/azureuser/soulmatch-ops/monitor-latest.json 2>/dev/null || true
echo '--- health endpoints ---'
for port in 3001 3002 3003 3004 3005 3006 3007 3011; do
  echo "port `$port"
  curl -fsS --max-time 5 http://127.0.0.1:`$port/health || true
  echo
done
echo '--- latest backups ---'
ls -1dt /home/azureuser/backups/soulmatch/20* 2>/dev/null | head -n 5 || true
"@
    Invoke-Remote $remoteStatus
}

function Sync-OfflineBackup {
    $script = Join-Path $PWD "tools\sync-azure-backups-to-windows.ps1"
    if (-not (Test-Path -LiteralPath $script)) {
        throw "Missing $script"
    }
    & powershell -ExecutionPolicy Bypass -File $script -Destination $LocalBackupDestination -Verify
}

switch ($Mode) {
    "Help" { Show-Help }
    "Local" { Setup-Local }
    "VM" { Setup-VM }
    "GitHub" { Setup-GitHub }
    "Status" { Show-Status }
    "OfflineBackup" { Sync-OfflineBackup }
    "All" {
        Setup-Local
        Setup-VM
        Setup-GitHub
        Sync-OfflineBackup
    }
}
