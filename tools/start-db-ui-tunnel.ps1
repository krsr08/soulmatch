param(
    [string]$VmHost = "20.204.142.19",
    [string]$VmUser = "azureuser",
    [string]$VmPort = "22",
    [string]$SshKeyPath = "$env:USERPROFILE\.ssh\soulmatch_github_deploy",
    [string]$DeployPath = "/home/azureuser/soulmatch",
    [int]$LocalPostgresPort = 15432,
    [int]$LocalMongoPort = 27018
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Write-Host "[$timestamp] $Message"
}

if (-not (Test-Path -LiteralPath $SshKeyPath)) {
    throw "SSH key not found: $SshKeyPath"
}

$postgresIp = ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" "cd '$DeployPath' && docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' docker-postgres-1"
$mongoIp = ssh -i $SshKeyPath -p $VmPort "$VmUser@$VmHost" "cd '$DeployPath' && docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' docker-mongodb-1"

if (-not $postgresIp) {
    throw "Could not find Postgres container IP on VM."
}
if (-not $mongoIp) {
    throw "Could not find MongoDB container IP on VM."
}

Write-Step "Starting secure DB tunnel. Keep this window open while using pgAdmin or MongoDB Compass."
Write-Host ""
Write-Host "Postgres connection for pgAdmin:"
Write-Host "  Host: localhost"
Write-Host "  Port: $LocalPostgresPort"
Write-Host "  Database: soulmatch_db"
Write-Host "  Username: soulmatch_user"
Write-Host "  Password: use POSTGRES_PASSWORD from VM docker/production.env"
Write-Host ""
Write-Host "MongoDB Compass connection:"
Write-Host "  mongodb://localhost:$LocalMongoPort/soulmatch_chat"
Write-Host ""
Write-Host "Press Ctrl+C in this window to stop the tunnel."
Write-Host ""

ssh -i $SshKeyPath `
    -p $VmPort `
    -N `
    -L "$LocalPostgresPort`:$postgresIp`:5432" `
    -L "$LocalMongoPort`:$mongoIp`:27017" `
    "$VmUser@$VmHost"
