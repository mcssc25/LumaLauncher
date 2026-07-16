$ErrorActionPreference = 'Stop'

$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$avdHome = Join-Path $PSScriptRoot '.emulator'
$apk = Join-Path $PSScriptRoot 'Luma-Launcher-v0.1-debug.apk'
$device = 'emulator-5570'

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

foreach ($required in @($emulator, $adb, $apk)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required file not found: $required"
    }
}

$env:ANDROID_AVD_HOME = $avdHome
$knownDevice = (& $adb devices) -match "^$device\s+"

if (-not $knownDevice) {
    Start-Process -FilePath $emulator `
        -ArgumentList '-avd LumaPreview -port 5570 -no-audio -gpu auto' `
        -WindowStyle Normal | Out-Null
}

Write-Host 'Starting the Android preview. The first launch can take about one minute...'
$ready = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    Start-Sleep -Seconds 2
    $deviceOnline = (& $adb devices) -match "^$device\s+device$"
    if (-not $deviceOnline) {
        continue
    }
    $booted = & $adb -s $device shell getprop sys.boot_completed
    if ($booted -eq '1') {
        $ready = $true
        break
    }
}

if (-not $ready) {
    throw 'The Android preview did not finish starting within three minutes.'
}

& $adb -s $device install -r $apk | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw 'Luma could not be installed in the preview.'
}

& $adb -s $device shell settings put secure immersive_mode_confirmations confirmed | Out-Null
& $adb -s $device shell cmd package set-home-activity com.lumalauncher.app/.MainActivity | Out-Host
& $adb -s $device shell am start -a android.intent.action.MAIN -c android.intent.category.HOME | Out-Host
Write-Host 'Luma is open. Use the arrow keys and Enter to navigate.'
