$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$iconPath = Join-Path $repoRoot "app-icon.png"

$image = [System.Windows.Forms.Clipboard]::GetImage()
if ($null -eq $image) {
    throw "No image found in clipboard. Copy the exact app icon image first, then run this script again."
}

try {
    $image.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $image.Dispose()
}

& (Join-Path $PSScriptRoot "apply-android-app-icon.ps1") -Source $iconPath
