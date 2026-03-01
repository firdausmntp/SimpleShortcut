<div align="center">

# ⚡ SimpleShortcut

**A universal Android shortcut & deeplink launcher — open any app or URL from your home screen widget.**

[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208)-brightgreen?style=flat-square)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-blue?style=flat-square)](https://developer.android.com/about/versions/14)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Release](https://img.shields.io/github/v/release/YOUR_GITHUB_USER/SimpleShortcut?style=flat-square&logo=github)](../../releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)

</div>

---

## What is SimpleShortcut?

SimpleShortcut lets you **create one-tap shortcuts to any deeplink, URL, or app** on your Android device. All shortcuts are displayed in a scrollable **home screen widget** and also available as **dynamic launcher shortcuts** (long-press the app icon).

No root, no ADB, no setup. Just add a shortcut and tap.

---

## ✨ Features

| Feature | Description |
|---|---|
| **Universal Intent Launcher** | Open any app by package name (`com.whatsapp`, `com.gojek.app`, etc.) |
| **Full Deeplink Support** | Custom schemes (`gojek://`, `gopay://`), HTTPS URLs, or bare package launch |
| **Home Screen Widget (4×2)** | Compact, scrollable `AppWidget` with a built-in **"+ Add"** button |
| **Home Screen Widget (1×1)** | Single-icon widget — pick one shortcut per widget instance |
| **Dynamic App Shortcuts** | Long-press the app icon → up to 5 shortcuts appear in the popup |
| **Play Store Fallback** | Opens Play Store automatically if the target app isn't installed |
| **Emoji Avatars** | Give every shortcut a custom emoji icon |
| **Offline & Persistent** | All data stored locally in Room (SQLite). No internet required |
| **Material Design 3** | Clean, modern UI following Google's latest design guidelines |

---

## 📋 Supported Intent Modes

SimpleShortcut handles 3 distinct shortcut modes:

```
┌─────────────────────────────────────────────────────────────────┐
│  Package Name  │  Deeplink / URL  │  Result                     │
├─────────────────────────────────────────────────────────────────┤
│  com.x.app     │  scheme://path   │  Open specific screen in app │
│  com.x.app     │  (empty)         │  Open home screen of app     │
│  (empty)       │  https://...     │  Open URL in browser         │
│  (empty)       │  scheme://...    │  Broadcast to any handler    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.23
- **Build**: Gradle 9.x + AGP 8.3.2
- **Database**: Room + Coroutines `Flow` (reactive real-time updates)
- **UI**: Material Design 3 — `MaterialCardView`, `FloatingActionButton`, `MaterialAlertDialogBuilder`
- **Widget**: `AppWidgetProvider` + `RemoteViewsService` + `RemoteViewsFactory`
- **Shortcuts**: `ShortcutManager` API 26+

---

## 🏗️ Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── layout/
│   │   ├── activity_main.xml        ← RecyclerView + FAB
│   │   ├── item_shortcut.xml        ← List item card
│   │   ├── dialog_add_shortcut.xml  ← Add/edit form
│   │   ├── widget_layout.xml        ← Widget root + add button
│   │   └── widget_item.xml          ← Widget row item
│   ├── xml/
│   │   └── shortcut_widget_info.xml ← AppWidget provider config
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
└── kotlin/com/josski/simpleshortcut/
    ├── SimpleShortcutApp.kt         ← Application class
    ├── MainActivity.kt
    ├── ShortcutAdapter.kt
    ├── ShortcutViewModel.kt
    ├── DeeplinkLauncher.kt          ← Universal intent resolver
    ├── data/
    │   ├── Shortcut.kt              ← @Entity Room model
    │   ├── ShortcutDao.kt           ← DAO (Flow + sync)
    │   ├── ShortcutDatabase.kt      ← Room singleton
    │   └── ShortcutRepository.kt    ← Single source of truth
    └── widget/
        ├── ShortcutWidgetProvider.kt
        ├── ShortcutWidgetService.kt
        ├── ShortcutRemoteViewsFactory.kt
        └── ShortcutPublisher.kt     ← Dynamic shortcuts sync
```

---

## 🚀 Build & Run

### Requirements

- **Android Studio** (Hedgehog+) or **VS Code** with Kotlin/Java extension
- **JDK 17+**
- Android SDK with API 34 platform installed

### Debug Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Signed)

1. Generate a keystore (one-time):
   ```bash
   keytool -genkey -v -keystore keystore.jks -alias mykey \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Fill in `gradle.properties` (this file is gitignored):
   ```properties
   KEYSTORE_PATH=../keystore.jks
   KEYSTORE_PASS=your_store_password
   KEY_ALIAS=mykey
   KEY_PASS=your_key_password
   ```
3. Build:
   ```bash
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```

### Install to Device

```bash
./gradlew installDebug
```

---

## 🔍 How to Find Package Names & Deeplinks

**Package Name** — The unique ID of an Android app:
- From Play Store URL: `https://play.google.com/store/apps/details?id=`**`com.whatsapp`**
- On device: *Settings → Apps → [App] → App Info* shows the package name on most launchers

**Deeplinks** — Custom URI schemes registered by apps:
```bash
# Extract from an APK using apktool
apktool d target.apk -o target_decoded
grep -r "scheme" target_decoded/AndroidManifest.xml

# For native apps
strings classes.dex | grep "://

# For Flutter apps
strings lib/arm64-v8a/libapp.so | grep "://"
```

**Common examples:**

| App | Deeplink | Package |
|---|---|---|
| WhatsApp Chat | `whatsapp://send?phone=628xx` | `com.whatsapp` |
| YouTube Video | `youtube://watch?v=VIDEO_ID` | `com.google.android.youtube` |
| GoPay Topup | `gopay://topup` | `com.gojek.gopay` |
| GoPay Scan QR | `gopay://scanqr` | `com.gojek.gopay` |
| Gojek Home | `gojek://home` | `com.gojek.app` |

---

## 🔐 Permissions

| Permission | Reason |
|---|---|
| `QUERY_ALL_PACKAGES` | Required on Android 11+ to detect whether any arbitrary app is installed and resolve its intents |

> **Note**: This permission may require justification if publishing to the Google Play Store. For sideloading / personal use it is unrestricted.

---

## 🚀 Automated Release (GitHub Actions)

Every time you push a version tag the workflow builds a **signed APK** and publishes it as a GitHub Release automatically.

```bash
git tag v0.0.1
git push origin v0.0.1
# → GitHub Actions builds, signs, and creates Release with APK download
```

### Required GitHub Secrets

Set these in your repo → **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w 0 keystore.jks` (Linux/macOS) or PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks"))` |
| `KEYSTORE_PASS` | `android` |
| `KEY_ALIAS` | `simpleshortcut_key` |
| `KEY_PASS` | `android` |

> Replace `YOUR_GITHUB_USER` in the Release badge at the top of this README with your actual GitHub username.

---

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## 📜 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.
