param(
    [string]$PackageName = "com.soulmatch.app",
    [string]$AndroidProject = "android",
    [string]$OutputDir = "api-testing/device-smoke",
    [string]$DummyMobile = "9999999999",
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    & adb @Args
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Get-OnlyDevice {
    $lines = @((& adb devices) | Where-Object { $_ -match "`tdevice$" })
    if ($lines.Count -eq 0) {
        throw "No Android device connected. Enable USB debugging and accept the device authorization prompt."
    }
    if ($lines.Count -gt 1) {
        throw "Multiple Android devices connected. Connect only one device for this smoke test."
    }
    return ($lines[0] -split "`t")[0]
}

function Save-State {
    param([string]$Name)

    $remotePng = "/sdcard/soulmatch-$Name.png"
    $remoteXml = "/sdcard/soulmatch-$Name.xml"
    $localPng = Join-Path $OutputDir "$Name.png"
    $localXml = Join-Path $OutputDir "$Name.xml"
    $localCrash = Join-Path $OutputDir "$Name-crash.log"

    Invoke-Adb shell screencap '-p' $remotePng | Out-Null
    Invoke-Adb pull $remotePng $localPng | Out-Null
    Invoke-Adb shell uiautomator dump $remoteXml | Out-Null
    Invoke-Adb pull $remoteXml $localXml | Out-Null
    & adb shell logcat -d -b crash > $localCrash

    return @{
        Png = $localPng
        Xml = $localXml
        Crash = $localCrash
    }
}

function Get-CenterFromBounds {
    param([string]$Bounds)
    if ($Bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
        throw "Cannot parse bounds: $Bounds"
    }
    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    return @($x, $y)
}

function Get-NodeCenterByText {
    param(
        [string]$XmlPath,
        [string]$Text
    )
    [xml]$xml = Get-Content -LiteralPath $XmlPath -Raw
    $node = $xml.SelectSingleNode("//*[@text='$Text']")
    if ($null -eq $node) {
        throw "Could not find UI text '$Text' in $XmlPath"
    }
    return Get-CenterFromBounds $node.bounds
}

function Get-MobileFieldCenter {
    param([string]$XmlPath)
    [xml]$xml = Get-Content -LiteralPath $XmlPath -Raw
    $editTexts = @($xml.SelectNodes("//*[@class='android.widget.EditText']"))
    $candidate = $editTexts |
        Where-Object { $_.text -ne "+91" } |
        Sort-Object {
            if ($_.bounds -match "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
                [int]$Matches[3] - [int]$Matches[1]
            } else {
                0
            }
        } -Descending |
        Select-Object -First 1

    if ($null -eq $candidate) {
        throw "Could not find mobile number input field in $XmlPath"
    }
    return Get-CenterFromBounds $candidate.bounds
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$device = Get-OnlyDevice
Write-Host "Using Android device: $device"

if (-not $SkipInstall) {
    Write-Host "Installing debug build..."
    Push-Location $AndroidProject
    try {
        & .\gradlew.bat :app:installDebug --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle installDebug failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

Invoke-Adb shell logcat -c | Out-Null

Write-Host "Launching $PackageName..."
Invoke-Adb shell am force-stop $PackageName | Out-Null
Invoke-Adb shell monkey '-p' $PackageName '-c' android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 5

$appPid = (& adb shell pidof '-s' $PackageName).Trim()
if (-not $appPid) {
    throw "$PackageName is not running after launch."
}

$launch = Save-State "01-launch"
$loginCenter = Get-NodeCenterByText $launch.Xml "Log In"
Invoke-Adb shell input tap $loginCenter[0] $loginCenter[1] | Out-Null
Start-Sleep -Seconds 2

$mobile = Save-State "02-mobile-verification"
$mobileCenter = Get-MobileFieldCenter $mobile.Xml
Invoke-Adb shell input tap $mobileCenter[0] $mobileCenter[1] | Out-Null
Start-Sleep -Milliseconds 500
Invoke-Adb shell input text $DummyMobile | Out-Null
Start-Sleep -Seconds 1

$filled = Save-State "03-mobile-filled"
[xml]$filledXml = Get-Content -LiteralPath $filled.Xml -Raw
$hasSendOtp = $null -ne $filledXml.SelectSingleNode("//*[@text='Send OTP']")
$hasNumber = $null -ne $filledXml.SelectSingleNode("//*[@text='$DummyMobile']")
$crashText = Get-Content -LiteralPath $filled.Crash -Raw

$report = @"
# SoulMatch Physical Device Smoke Test

Device: $device
Package: $PackageName
Dummy mobile entered: $DummyMobile

## Checks

| Check | Result |
|---|---|
| App launched | PASS |
| App process running | PASS |
| Login CTA found and tapped | PASS |
| Mobile verification screen opened | PASS |
| Mobile field accepted dummy input | $(if ($hasNumber) { "PASS" } else { "FAIL" }) |
| Send OTP CTA visible after input | $(if ($hasSendOtp) { "PASS" } else { "FAIL" }) |
| Crash log empty | $(if ([string]::IsNullOrWhiteSpace($crashText)) { "PASS" } else { "FAIL" }) |

Screenshots and UI XML files are in this folder.
"@

$reportPath = Join-Path $OutputDir "README.md"
$report | Set-Content -LiteralPath $reportPath

Write-Host "Smoke test complete: $reportPath"
