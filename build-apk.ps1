# Builds the installable APK.
#   .\build-apk.ps1            -> debug APK, signed with the Android debug key, ready to sideload
#   .\build-apk.ps1 -Release   -> release APK, UNSIGNED (needs your own keystore, see README)
param([switch]$Release)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "X:\Android\Sdk" }
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr" }
if (-not $env:NDK_HOME) {
	$env:NDK_HOME = (Get-ChildItem "$env:ANDROID_HOME\ndk" | Sort-Object Name -Descending | Select-Object -First 1).FullName
}

# without this the debug .so ships 128 MB of DWARF symbols and the APK is ~275 MB
$env:RUSTFLAGS = "-C strip=symbols $env:RUSTFLAGS".Trim()

# arm64 only: every Android phone/tablet made in the last decade. Add x86_64 for the emulator.
# AGP updates the APK zip in place and never reclaims the old .so, so each rebuild grows it
Remove-Item -Recurse -Force "$PSScriptRoot\src-tauri\gen\android\app\build\outputs\apk" -ErrorAction SilentlyContinue

$cmd = @("tauri", "android", "build", "--apk", "--target", "aarch64")
if (-not $Release) { $cmd += "--debug" }

pnpm @cmd
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Get-ChildItem "$PSScriptRoot\src-tauri\gen\android\app\build\outputs\apk" -Recurse -Filter *.apk |
	Sort-Object LastWriteTime -Descending | Select-Object -First 3 FullName, Length, LastWriteTime
