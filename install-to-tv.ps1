param(
    [Parameter(Mandatory = $false)]
    [string]$TvAddress
)

$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$apk = Join-Path $PSScriptRoot 'Luma-Launcher.apk'

if (-not (Test-Path -LiteralPath $adb)) {
    throw 'Android platform tools were not found. Install Android Studio or Android platform-tools first.'
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw 'The Luma APK is missing from this folder.'
}

if ($TvAddress) {
    Write-Host "Connecting to $TvAddress..."
    & $adb connect $TvAddress
}

Write-Host 'Installing Luma Launcher...'
& $adb install -r $apk

if ($LASTEXITCODE -ne 0) {
    throw 'Installation failed. Confirm that debugging is enabled and accept the TV authorization prompt.'
}

Write-Host 'Luma Launcher is installed. Press Home on the TV remote and select Luma Launcher.'
