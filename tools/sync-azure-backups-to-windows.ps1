param(
    [string]$StorageAccount = "soulmatchbkb1bbhwbi",
    [string]$Container = "soulmatch-backups",
    [string]$Destination = "$env:USERPROFILE\Documents\soulmatch-backups",
    [int]$RetentionDays = 180,
    [switch]$Verify
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

Require-Command "az"

New-Item -ItemType Directory -Force -Path $Destination | Out-Null

Write-Step "Syncing SoulMatch backups from Azure Blob."
Write-Step "Storage account: $StorageAccount"
Write-Step "Container: $Container"
Write-Step "Destination: $Destination"

az storage blob download-batch `
    --account-name $StorageAccount `
    --source $Container `
    --destination $Destination `
    --auth-mode key `
    --overwrite true `
    --only-show-errors | Out-Host

if ($Verify) {
    Write-Step "Verifying downloaded backup checksums."
    $manifests = Get-ChildItem -Path $Destination -Filter manifest.json -Recurse
    foreach ($manifest in $manifests) {
        $backupDir = $manifest.Directory.FullName
        $data = Get-Content -Raw -Path $manifest.FullName | ConvertFrom-Json
        foreach ($file in $data.files) {
            if ($file.name -eq "manifest.json") {
                Write-Step "Skipping legacy self-referential manifest entry: $($manifest.FullName)"
                continue
            }
            $filePath = Join-Path $backupDir $file.name
            if (-not (Test-Path -LiteralPath $filePath)) {
                throw "Missing backup file referenced by manifest: $filePath"
            }
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $filePath).Hash.ToLowerInvariant()
            if ($hash -ne $file.sha256.ToLowerInvariant()) {
                throw "Checksum mismatch for $filePath"
            }
        }
    }
    Write-Step "Checksum verification passed for $($manifests.Count) backup manifest(s)."
}

if ($RetentionDays -gt 0) {
    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -Path $Destination -Directory |
        Where-Object { $_.Name -match '^20\d{6}T\d{6}Z$' -and $_.LastWriteTime -lt $cutoff } |
        ForEach-Object {
            Write-Step "Deleting local backup older than $RetentionDays days: $($_.FullName)"
            Remove-Item -LiteralPath $_.FullName -Recurse -Force
        }
}

Write-Step "Offline backup sync complete."
