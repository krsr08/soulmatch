param(
    [string]$OutputDir = "security-reports",
    [switch]$FailOnFindings
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$reportDir = Join-Path $repoRoot $OutputDir
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $reportDir "security-audit-$timestamp.md"
$secretPath = Join-Path $reportDir "secret-scan-$timestamp.txt"
$auditPath = Join-Path $reportDir "dependency-audit-$timestamp.txt"

$secretPatterns = @(
    "AKIA[0-9A-Z]{16}",
    "AIza[0-9A-Za-z_\-]{20,}",
    "-----BEGIN (RSA |EC |OPENSSH |)PRIVATE KEY-----",
    "RAZORPAY_KEY_SECRET[[:space:]]*=",
    "JWT_SECRET[[:space:]]*=",
    "INTERNAL_SERVICE_SECRET[[:space:]]*=",
    "FIREBASE_PRIVATE_KEY[[:space:]]*=",
    "AWS_SECRET_ACCESS_KEY[[:space:]]*=",
    "TWILIO_AUTH_TOKEN[[:space:]]*=",
    "(password|secret|token|private_key)[[:space:]]*[:=][[:space:]]*['""][^'""]{12,}['""]"
)

$excludeArgs = @(
    ":(exclude)**/node_modules/**",
    ":(exclude)**/build/**",
    ":(exclude)**/.gradle/**",
    ":(exclude)docker/production.env",
    ":(exclude)android/local.properties",
    ":(exclude)**/*.example",
    ":(exclude)**/security-reports/**",
    ":(exclude)**/recovery-packages/**",
    ":(exclude)**/backups/**"
)

function Test-IsIntentionalPublicOrTemplateFinding {
    param([string]$Line)

    $normalized = $Line -replace "\\", "/"
    if ($normalized -match "^android/app/google-services\.json:") {
        return $true
    }

    if ($Line -match "process\.env\.") {
        return $true
    }

    if ($Line -match "REPLACE_WITH_|REPLACE_FROM_|change_this|placeholder|example\.com") {
        return $true
    }

    return $false
}

Push-Location $repoRoot
try {
    "SoulMatch security audit $timestamp" | Set-Content -LiteralPath $summaryPath
    "====================================" | Add-Content -LiteralPath $summaryPath
    "" | Add-Content -LiteralPath $summaryPath

    $secretFindings = @()
    foreach ($pattern in $secretPatterns) {
        $matches = & git grep -n -I -i -E -e $pattern -- . $excludeArgs 2>$null
        if ($LASTEXITCODE -eq 0 -and $matches) {
            $secretFindings += $matches |
                Where-Object { -not (Test-IsIntentionalPublicOrTemplateFinding $_) } |
                ForEach-Object {
                    ($_ -replace "(:)([^:]{0,6}).*$", '$1[REDACTED]')
                }
        }
    }
    $secretFindings = $secretFindings | Sort-Object -Unique
    if ($secretFindings.Count) {
        $secretFindings | Set-Content -LiteralPath $secretPath
    } else {
        "No high-confidence committed secret patterns found." | Set-Content -LiteralPath $secretPath
    }

    "## Secret Scan" | Add-Content -LiteralPath $summaryPath
    "- Report: $secretPath" | Add-Content -LiteralPath $summaryPath
    "- Findings: $($secretFindings.Count)" | Add-Content -LiteralPath $summaryPath
    "" | Add-Content -LiteralPath $summaryPath

    "Dependency audit output" | Set-Content -LiteralPath $auditPath
    "=======================" | Add-Content -LiteralPath $auditPath

    $packageRoots = @(
        (Join-Path $repoRoot "admin-web"),
        (Join-Path $repoRoot "backend")
    )
    $packageFiles = Get-ChildItem -Path $packageRoots -Recurse -Filter package-lock.json -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch "\\node_modules\\|\\build\\" }
    $dependencyFailures = 0
    foreach ($lock in $packageFiles) {
        $serviceDir = Split-Path $lock.FullName -Parent
        "" | Add-Content -LiteralPath $auditPath
        "### npm audit: $serviceDir" | Add-Content -LiteralPath $auditPath
        & npm --prefix $serviceDir audit --audit-level=high --omit=dev 2>&1 |
            Add-Content -LiteralPath $auditPath
        if ($LASTEXITCODE -ne 0) { $dependencyFailures++ }
    }

    "" | Add-Content -LiteralPath $auditPath
    "### Android dependency inventory" | Add-Content -LiteralPath $auditPath
    Push-Location (Join-Path $repoRoot "android")
    try {
        & .\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath --no-daemon 2>&1 |
            Add-Content -LiteralPath $auditPath
        if ($LASTEXITCODE -ne 0) { $dependencyFailures++ }
    } finally {
        Pop-Location
    }

    "## Dependency Audit" | Add-Content -LiteralPath $summaryPath
    "- Report: $auditPath" | Add-Content -LiteralPath $summaryPath
    "- Package lock files scanned: $($packageFiles.Count)" | Add-Content -LiteralPath $summaryPath
    "- Audit failures: $dependencyFailures" | Add-Content -LiteralPath $summaryPath
    "" | Add-Content -LiteralPath $summaryPath

    if ($FailOnFindings -and ($secretFindings.Count -gt 0 -or $dependencyFailures -gt 0)) {
        throw "Security audit found $($secretFindings.Count) secret finding(s) and $dependencyFailures dependency audit failure(s)."
    }

    Write-Host "Security audit complete: $summaryPath"
} finally {
    Pop-Location
}
