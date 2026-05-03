param(
    [Parameter(Mandatory = $true)]
    [string]$RecoveryPackage,

    [string]$VmHost = "20.204.142.19",
    [string]$VmUser = "azureuser",
    [string]$VmPort = "22",
    [string]$SshKeyPath = "$env:USERPROFILE\.ssh\soulmatch_github_deploy",
    [string]$DeployPath = "/home/azureuser/soulmatch",
    [string]$RemoteBackupRoot = "/home/azureuser/backups/soulmatch",

    [switch]$RestoreData,
    [switch]$UseProductionEnvFromPackage,
    [switch]$InstallPrerequisites
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Write-Host "[$timestamp] $Message"
}

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function Invoke-Remote {
    param([string]$Command)
    ssh -i $SshKeyPath -p $VmPort -o BatchMode=yes "$VmUser@$VmHost" $Command
}

function Copy-ToRemote {
    param(
        [string]$LocalPath,
        [string]$RemotePath
    )
    scp -i $SshKeyPath -P $VmPort $LocalPath "$VmUser@$VmHost`:$RemotePath"
}

Require-Command "ssh"
Require-Command "scp"
Require-Command "tar"

if (-not (Test-Path -LiteralPath $SshKeyPath)) {
    throw "SSH key not found: $SshKeyPath"
}
if (-not (Test-Path -LiteralPath $RecoveryPackage)) {
    throw "Recovery package not found: $RecoveryPackage"
}

$packagePath = (Resolve-Path -LiteralPath $RecoveryPackage).Path
$manifestPath = Join-Path $packagePath "recovery-manifest.json"
if (-not (Test-Path -LiteralPath $manifestPath)) {
    throw "Missing recovery manifest: $manifestPath"
}

$manifest = Get-Content -Raw -Path $manifestPath | ConvertFrom-Json
$sourceArchive = Join-Path $packagePath $manifest.sourceArchive
if (-not (Test-Path -LiteralPath $sourceArchive)) {
    throw "Missing source archive: $sourceArchive"
}

$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceArchive).Hash.ToLowerInvariant()
if ($sourceHash -ne $manifest.sourceArchiveSha256.ToLowerInvariant()) {
    throw "Source archive checksum mismatch. Package may be damaged: $sourceArchive"
}

if ($InstallPrerequisites) {
    Write-Step "Installing VM prerequisites."
    $prepScript = @"
set -Eeuo pipefail
sudo apt-get update
sudo apt-get install -y curl ca-certificates gnupg nginx rsync
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker `$(whoami) || true
fi
docker compose version >/dev/null
mkdir -p '$DeployPath'
"@
    $prepScript | ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" "bash -s"
} else {
    Write-Step "Checking VM Docker availability."
    Invoke-Remote "docker compose version >/dev/null && mkdir -p '$DeployPath'"
}

Write-Step "Uploading source archive to VM."
Copy-ToRemote -LocalPath $sourceArchive -RemotePath "/tmp/soulmatch-source.tar.gz"

$productionEnvPath = Join-Path $packagePath "production.env"
if ($UseProductionEnvFromPackage) {
    if (-not (Test-Path -LiteralPath $productionEnvPath)) {
        throw "Package does not contain production.env. Recreate package with -IncludeProductionEnv, or keep docker/production.env on the VM."
    }
    Write-Step "Uploading production.env from recovery package."
    Copy-ToRemote -LocalPath $productionEnvPath -RemotePath "/tmp/soulmatch-production.env"
}

$remoteDeployScript = @"
set -Eeuo pipefail
mkdir -p '$DeployPath'
tar -xzf /tmp/soulmatch-source.tar.gz -C '$DeployPath'
rm -f /tmp/soulmatch-source.tar.gz
if [ -f /tmp/soulmatch-production.env ]; then
  mkdir -p '$DeployPath/docker'
  mv /tmp/soulmatch-production.env '$DeployPath/docker/production.env'
  chmod 600 '$DeployPath/docker/production.env'
fi
if [ ! -f '$DeployPath/docker/production.env' ]; then
  echo 'Missing production env: $DeployPath/docker/production.env'
  echo 'Either keep it on the VM or rebuild with -UseProductionEnvFromPackage.'
  exit 30
fi
cd '$DeployPath'
find tools -type f -name '*.sh' -exec sed -i 's/\r$//' {} \; -exec chmod +x {} \;
DEPLOYED_SOURCE_COMMIT='$($manifest.commit)' DEPLOYED_SOURCE_BRANCH='$($manifest.branch)' bash tools/deploy-production.sh
"@

Write-Step "Deploying source on VM."
$remoteDeployScript | ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" "bash -s"

if ($RestoreData) {
    $dataRoot = Join-Path $packagePath "data-backup"
    if (-not (Test-Path -LiteralPath $dataRoot)) {
        throw "Recovery package has no data-backup folder."
    }

    $backupDir = Get-ChildItem -Path $dataRoot -Directory |
        Where-Object {
            (Test-Path -LiteralPath (Join-Path $_.FullName "postgres.dump")) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName "mongodb.archive.gz"))
        } |
        Sort-Object Name -Descending |
        Select-Object -First 1

    if (-not $backupDir) {
        throw "No valid backup directory found under $dataRoot"
    }

    $backupArchive = Join-Path $env:TEMP ("soulmatch-data-backup-" + $backupDir.Name + ".tar.gz")
    if (Test-Path -LiteralPath $backupArchive) {
        Remove-Item -LiteralPath $backupArchive -Force
    }

    Write-Step "Packing data backup $($backupDir.Name)."
    tar -czf $backupArchive -C $backupDir.FullName .

    Write-Step "Uploading data backup to VM."
    Copy-ToRemote -LocalPath $backupArchive -RemotePath "/tmp/soulmatch-data-backup.tar.gz"
    Remove-Item -LiteralPath $backupArchive -Force

    $remoteBackupPath = "$RemoteBackupRoot/$($backupDir.Name)"
    $remoteRestoreScript = @"
set -Eeuo pipefail
mkdir -p '$remoteBackupPath'
tar -xzf /tmp/soulmatch-data-backup.tar.gz -C '$remoteBackupPath'
rm -f /tmp/soulmatch-data-backup.tar.gz
cd '$DeployPath'
CONFIRM_RESTORE=yes bash tools/restore-production.sh '$remoteBackupPath'
"@
    Write-Step "Restoring data backup on VM."
    $remoteRestoreScript | ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" "bash -s"
}

Write-Step "Running final health checks."
Invoke-Remote "curl -fsS http://127.0.0.1:3001/health >/dev/null && curl -fsS http://127.0.0.1:3002/health >/dev/null && curl -fsS http://127.0.0.1:3003/health >/dev/null && curl -fsS http://127.0.0.1:3004/health >/dev/null && curl -fsS http://127.0.0.1:3005/health >/dev/null && curl -fsS http://127.0.0.1:3006/health >/dev/null && curl -fsS http://127.0.0.1:3007/health >/dev/null && curl -fsS http://127.0.0.1:3011/health >/dev/null"

Write-Step "Cloud rebuild completed successfully."
Write-Host "Deployed commit: $($manifest.commit)"
