param(
    [string]$Source = "app-icon.png"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = if ([System.IO.Path]::IsPathRooted($Source)) {
    $Source
} else {
    Join-Path $repoRoot $Source
}

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Icon source not found: $sourcePath"
}

Add-Type -AssemblyName System.Drawing

function Resize-Png {
    param(
        [Parameter(Mandatory = $true)][string]$InputPath,
        [Parameter(Mandatory = $true)][string]$OutputPath,
        [Parameter(Mandatory = $true)][int]$Size
    )

    $src = [System.Drawing.Image]::FromFile($InputPath)
    try {
        $dest = New-Object System.Drawing.Bitmap $Size, $Size
        try {
            $dest.SetResolution($src.HorizontalResolution, $src.VerticalResolution)
            $graphics = [System.Drawing.Graphics]::FromImage($dest)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.DrawImage($src, 0, 0, $Size, $Size)
            } finally {
                $graphics.Dispose()
            }

            $parent = Split-Path -Parent $OutputPath
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
            $dest.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $dest.Dispose()
        }
    } finally {
        $src.Dispose()
    }
}

$resRoot = Join-Path $repoRoot "android\app\src\main\res"
$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $sizes.GetEnumerator()) {
    $dir = Join-Path $resRoot $entry.Key
    Resize-Png -InputPath $sourcePath -OutputPath (Join-Path $dir "ic_launcher.png") -Size $entry.Value
    Resize-Png -InputPath $sourcePath -OutputPath (Join-Path $dir "ic_launcher_round.png") -Size $entry.Value
}

$drawableNoDpi = Join-Path $resRoot "drawable-nodpi"
New-Item -ItemType Directory -Force -Path $drawableNoDpi | Out-Null
Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $drawableNoDpi "app_icon_exact.png") -Force
Resize-Png -InputPath $sourcePath -OutputPath (Join-Path $drawableNoDpi "app_icon_splash.png") -Size 512

$adaptiveDir = Join-Path $resRoot "mipmap-anydpi-v26"
if (Test-Path -LiteralPath $adaptiveDir) {
    Remove-Item -LiteralPath (Join-Path $adaptiveDir "ic_launcher.xml") -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $adaptiveDir "ic_launcher_round.xml") -Force -ErrorAction SilentlyContinue
}

$themeV31 = Join-Path $resRoot "values-v31\themes.xml"
if (Test-Path -LiteralPath $themeV31) {
    $theme = Get-Content -LiteralPath $themeV31 -Raw
    $theme = $theme -replace '@drawable/ic_soulmatch_splash', '@drawable/app_icon_splash'
    Set-Content -LiteralPath $themeV31 -Value $theme -NoNewline
}

$theme = Join-Path $resRoot "values\themes.xml"
if (Test-Path -LiteralPath $theme) {
    $themeContent = Get-Content -LiteralPath $theme -Raw
    $themeContent = $themeContent -replace '@color/splash_background', '@drawable/splash_window_background'
    Set-Content -LiteralPath $theme -Value $themeContent -NoNewline
}

$splashWindow = Join-Path $resRoot "drawable\splash_window_background.xml"
if (Test-Path -LiteralPath $splashWindow) {
    $splashContent = Get-Content -LiteralPath $splashWindow -Raw
    $splashContent = $splashContent -replace '@drawable/ic_soulmatch_splash', '@drawable/app_icon_splash'
    Set-Content -LiteralPath $splashWindow -Value $splashContent -NoNewline
}

Write-Host "Android launcher icons generated from exact source: $sourcePath"
