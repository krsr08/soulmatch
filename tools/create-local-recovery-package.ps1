param(
    [string]$OutputRoot = "$env:USERPROFILE\Documents\soulmatch-recovery",
    [string]$BackupRoot = "$env:USERPROFILE\Documents\soulmatch-backups",
    [string]$ProductionEnvPath = ".\docker\production.env",
    [switch]$IncludeProductionEnv,
    [switch]$SkipBackupCopy,
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

function Get-LatestBackupDirectory {
    param([string]$Root)

    if (-not (Test-Path -LiteralPath $Root)) {
        return $null
    }

    Get-ChildItem -Path $Root -Directory -Recurse |
        Where-Object {
            $_.Name -match '^20\d{6}T\d{6}Z$' -and
            (Test-Path -LiteralPath (Join-Path $_.FullName "postgres.dump")) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName "mongodb.archive.gz"))
        } |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

Require-Command "git"

$repoRoot = (git rev-parse --show-toplevel).Trim()
Set-Location $repoRoot

$status = (git status --porcelain)
if ($status -and -not $AllowDirty) {
    throw "Working tree has uncommitted changes. Commit/push first, or rerun with -AllowDirty if you only want the current HEAD archive."
}

$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
$commit = (git rev-parse HEAD).Trim()
$branch = (git branch --show-current).Trim()
$shortCommit = $commit.Substring(0, 12)
$packageDir = Join-Path $OutputRoot "$timestamp-$shortCommit"

New-Item -ItemType Directory -Force -Path $packageDir | Out-Null

Write-Step "Creating recovery package: $packageDir"

$sourceArchive = Join-Path $packageDir "soulmatch-source-$shortCommit.tar.gz"
git archive --format=tar.gz --output $sourceArchive HEAD

$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceArchive).Hash.ToLowerInvariant()

$dataBackupCopied = $false
$dataBackupName = $null
if (-not $SkipBackupCopy) {
    $latestBackup = Get-LatestBackupDirectory -Root $BackupRoot
    if ($latestBackup) {
        $dataBackupRoot = Join-Path $packageDir "data-backup"
        New-Item -ItemType Directory -Force -Path $dataBackupRoot | Out-Null
        Copy-Item -LiteralPath $latestBackup.FullName -Destination $dataBackupRoot -Recurse -Force
        $dataBackupCopied = $true
        $dataBackupName = $latestBackup.Name
        Write-Step "Copied latest local data backup: $($latestBackup.FullName)"
    } else {
        Write-Step "No local DB backup found under $BackupRoot. Package will contain source only."
    }
}

$productionEnvCopied = $false
if ($IncludeProductionEnv) {
    $resolvedProductionEnv = Resolve-Path -LiteralPath $ProductionEnvPath -ErrorAction SilentlyContinue
    if (-not $resolvedProductionEnv) {
        throw "Production env file not found: $ProductionEnvPath"
    }

    Copy-Item -LiteralPath $resolvedProductionEnv.Path -Destination (Join-Path $packageDir "production.env") -Force
    $productionEnvCopied = $true
    Write-Step "Copied production.env into the recovery package. Keep this package private."
}

$manifest = [ordered]@{
    packageCreatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    repoRoot = $repoRoot
    branch = $branch
    commit = $commit
    sourceArchive = (Split-Path -Leaf $sourceArchive)
    sourceArchiveSha256 = $sourceHash
    dataBackupCopied = $dataBackupCopied
    dataBackupName = $dataBackupName
    productionEnvCopied = $productionEnvCopied
    notes = "This package can rebuild the VM source. Use data-backup to restore Postgres, MongoDB, and profile uploads."
}

$manifestPath = Join-Path $packageDir "recovery-manifest.json"
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $manifestPath -Encoding ascii

Write-Step "Recovery package ready."
Write-Host ""
Write-Host "Package: $packageDir"
Write-Host "Commit:  $commit"
if ($dataBackupCopied) {
    Write-Host "Data:    included backup $dataBackupName"
} else {
    Write-Host "Data:    not included"
}
if ($productionEnvCopied) {
    Write-Host "Secrets: production.env included. Do not upload this package to Git."
} else {
    Write-Host "Secrets: production.env not included"
}
