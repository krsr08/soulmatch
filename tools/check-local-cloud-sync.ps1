param(
    [string]$VmHost = "20.204.142.19",
    [string]$VmUser = "azureuser",
    [string]$VmPort = "22",
    [string]$SshKeyPath = "$env:USERPROFILE\.ssh\soulmatch_github_deploy",
    [string]$DeployPath = "/home/azureuser/soulmatch",
    [string]$BackupRoot = "$env:USERPROFILE\Documents\soulmatch-backups",
    [switch]$AllowDirty
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

Require-Command "git"
Require-Command "ssh"

if (-not (Test-Path -LiteralPath $SshKeyPath)) {
    throw "SSH key not found: $SshKeyPath"
}

$repoRoot = (git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

$localCommit = (git rev-parse HEAD).Trim()
$localBranch = (git branch --show-current).Trim()
$dirty = [bool](git status --porcelain)

if ($dirty -and -not $AllowDirty) {
    throw "Local working tree has uncommitted changes. Commit first so local and cloud can be compared exactly."
}

Write-Step "Reading deployed version from cloud."
$remoteJson = ssh -i $SshKeyPath -p $VmPort -o BatchMode=yes "$VmUser@$VmHost" "cat '$DeployPath/.soulmatch-deployed-version.json' 2>/dev/null || true"
if (-not $remoteJson) {
    throw "Cloud deployed version file is missing. Redeploy once with the updated deploy script."
}

$remoteVersion = $remoteJson | ConvertFrom-Json
$remoteCommit = [string]$remoteVersion.sourceCommit

Write-Host ""
Write-Host "Local branch:  $localBranch"
Write-Host "Local commit:  $localCommit"
Write-Host "Cloud branch:  $($remoteVersion.sourceBranch)"
Write-Host "Cloud commit:  $remoteCommit"
Write-Host "Cloud deploy:  $($remoteVersion.deployedAt)"

if ($localCommit -eq $remoteCommit) {
    Write-Host "Code sync:     OK" -ForegroundColor Green
} else {
    Write-Host "Code sync:     MISMATCH" -ForegroundColor Red
}

Write-Step "Checking cloud service health."
ssh -i $SshKeyPath -p $VmPort -o BatchMode=yes "$VmUser@$VmHost" "curl -fsS http://127.0.0.1:3001/health >/dev/null && curl -fsS http://127.0.0.1:3002/health >/dev/null && curl -fsS http://127.0.0.1:3003/health >/dev/null && curl -fsS http://127.0.0.1:3004/health >/dev/null && curl -fsS http://127.0.0.1:3005/health >/dev/null && curl -fsS http://127.0.0.1:3006/health >/dev/null && curl -fsS http://127.0.0.1:3007/health >/dev/null && curl -fsS http://127.0.0.1:3011/health >/dev/null"
Write-Host "Cloud health:  OK" -ForegroundColor Green

$latestBackup = $null
if (Test-Path -LiteralPath $BackupRoot) {
    $latestBackup = Get-ChildItem -Path $BackupRoot -Directory -Recurse |
        Where-Object {
            $_.Name -match '^20\d{6}T\d{6}Z$' -and
            (Test-Path -LiteralPath (Join-Path $_.FullName "postgres.dump")) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName "mongodb.archive.gz"))
        } |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

if ($latestBackup) {
    Write-Host "Laptop backup: $($latestBackup.Name)" -ForegroundColor Green
} else {
    Write-Host "Laptop backup: NOT FOUND under $BackupRoot" -ForegroundColor Yellow
}

if ($localCommit -ne $remoteCommit) {
    exit 1
}
