# Foundry Lightweight Client (FLC) — Android fork

Fork of [phenomen/flc](https://github.com/phenomen/flc) that also builds as an installable
Android APK for phones and tablets, running Foundry in true fullscreen (immersive mode).

What changed against upstream:

- **Android target** (`src-tauri/gen/android`) with `build-apk.ps1` and a GitHub Actions workflow.
- **Immersive fullscreen**, screen kept awake, mixed content and media autoplay allowed
  (`MainActivity.kt`).
- **In-app updates outside the Play Store** (`Updater.kt`): checks a manifest on GitHub Releases at
  startup, asks, downloads the APK, hands it to the system installer.
- **Joining a server navigates the single webview** instead of opening a new window — Android has
  no multi-window webview. The **back button/gesture returns to the server list**.
- Mobile UI: no server launcher tab (no NodeJS on Android), no global shortcuts, no desktop
  updater, touch-friendly and notch-safe layout.

Desktop builds still work exactly as upstream.

- **Upstream website:** https://foundry.ruleplaying.com/flc
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)

## Android

### Build the APK

```powershell
.\build-apk.ps1
```

Output: `src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk`.

It is a debug-variant APK, which means it is signed with the local Android debug key and installs
by sideloading. `.\build-apk.ps1 -Release` produces a smaller release APK, but it is unsigned —
sign it yourself with `apksigner` and your own keystore before installing.

Requires the Android SDK (platform 36 + build-tools), an NDK, a JDK 17+ (the one bundled with
Android Studio works) and the `aarch64-linux-android` Rust target. Override `ANDROID_HOME`,
`JAVA_HOME` or `NDK_HOME` if yours live elsewhere.

Pushing a `v*` tag (or running the workflow manually) builds the same APK in CI and uploads it as
a build artifact.

### Install

```powershell
adb install -r .\src-tauri\gen\android\app\build\outputs\apk\universal\debug\app-universal-debug.apk
```

Or copy the APK to the device and open it (needs "install unknown apps" for the file manager).

### Signing

Every APK — debug or release, local or CI — is signed with the key described by `keystore.properties`
at the repo root (both it and `flc.keystore` are gitignored). **Back that keystore up.** Android
refuses to install an APK over one signed with a different key, so losing it means every user has to
uninstall and reinstall, losing their server list.

Recreating it from scratch:

```bash
keytool -genkeypair -keystore flc.keystore -alias flc -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=FLC Mobile, O=flc-mobile, C=BR"
```

Then write `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
Without it the build still works, but Gradle falls back to the throwaway Android debug key.

### Updates

`Updater.kt` fetches `android-latest.json` from the latest GitHub release at startup. If its
`versionCode` is higher than the installed one it offers the update, downloads the APK with the
system `DownloadManager`, and opens the system installer. Android asks for "install unknown apps"
once. Offline or unreachable manifest is silently ignored.

Shipping a new version:

1. Bump `version` in `src-tauri/tauri.conf.json` (`versionCode` is derived from it:
   `major*1000000 + minor*1000 + patch`).
2. Tag it: `git tag v7.9.5 && git push --tags`.
3. CI builds the APK, writes `android-latest.json` and publishes both on the release. Installed apps
   pick it up on the next launch.

CI needs two repository secrets to sign with the same key: `KEYSTORE_BASE64` and
`KEYSTORE_PASSWORD` (the password is in your local `keystore.properties`). Without them the workflow
still builds, but the APK cannot update anything.

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("flc.keystore")) | Set-Clipboard
```

The updater URL lives at the top of `Updater.kt` — point it at your own repo if you rename it.

## Features

### Fast and Bloat-free

No browser bloat saves valuable resources. The entire app is just 5MB and uses 60% less RAM than Google Chrome.

### Secure and Open-Source

All your data is stored locally. No analytics and statistics are collected. Source code is available on GitHub.

### High Compatibility

Works with local servers, IPs, VPN, http and https URLs, and official Foundry VTT hosting providers.

### Server Launcher

NodeJS server is an efficient way to run local Foundry server. It requires significantly less resources than a standalone Electron-based client.

## Installation

Download the latest release for your platform from [GitHub Releases](https://github.com/phenomen/flc/releases) or [Website](https://foundry.ruleplaying.com/flc).

## Development

### Prerequisites

- [Rust Toolchain](https://www.rust-lang.org/)
- [Tauri Prerequisites](https://tauri.app/v1/guides/getting-started/prerequisites)

### Setup

1. Clone the repository:

```bash
git clone https://github.com/phenomen/flc.git
cd flc
```

2. Install dependencies:

```bash
pnpm install
```

3. Run in development mode:

```bash
pnpm tauri dev
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.

## Acknowledgments

- [Tauri](https://tauri.app/) - Rust-powered backend
- [Svelte](https://svelte.dev/) - Frontend UI framework
- [Tailwind CSS](https://tailwindcss.com/) - CSS framework
- [shadcn-svelte](https://www.shadcn-svelte.com/) - UI components
- [Lucide](https://lucide.dev/) - Icons
