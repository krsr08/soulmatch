param(
    [switch]$BuildAndroid,
    [switch]$StartDocker,
    [switch]$SkipChecks
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

function Write-Step($Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Require-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

Set-Location $RepoRoot

Write-Step "SoulMatch local power-up started"
Write-Host "Repo: $RepoRoot"

if (-not $SkipChecks) {
    Write-Step "Checking required tools"
    Require-Command git
    Require-Command docker
    Require-Command node
    Require-Command npm
    Require-Command java
}

Write-Step "Checking important files"
$importantFiles = @(
    "android\app\google-services.json",
    "android\local.properties",
    "docker\docker-compose.dev.yml",
    "docker\docker-compose.prod.yml",
    "database\schema.sql"
)

foreach ($file in $importantFiles) {
    if (Test-Path $file) {
        Write-Host "OK  $file" -ForegroundColor Green
    } else {
        Write-Host "MISS $file" -ForegroundColor Yellow
    }
}

Write-Step "Git status"
git status --short

if ($StartDocker) {
    Write-Step "Starting local Docker services"
    docker compose -f docker/docker-compose.dev.yml up -d
    docker compose -f docker/docker-compose.dev.yml ps
}

if ($BuildAndroid) {
    Write-Step "Building Android debug APK"
    Push-Location android
    .\gradlew.bat :app:assembleDebug --no-daemon
    Pop-Location
}

Write-Step "Done"
Write-Host "Use Android Studio or run: cd android; .\gradlew.bat :app:installDebug --no-daemon"

