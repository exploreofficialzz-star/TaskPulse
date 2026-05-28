# TaskPulse 🚀

**Command-based Android automation by chAs Technologies LLC**

> Type what you want. Your phone obeys.

---

## Overview

TaskPulse is a fully offline, rule-based Android app that lets users control their phone through natural language commands. No AI APIs, no cloud backend — just fast on-device automation.

## Features

- 📱 **Natural Language Commands** — "Block Instagram for 2 hours"
- 🚫 **App Blocking** — Full-screen overlay enforcement
- 🔔 **Notification Control** — Pause, filter, schedule
- ⏱️ **Focus Modes** — Study, Work, Sleep, Focus
- 🎯 **Rule Engine** — IF/THEN automation rules
- 💰 **Points System** — Fair usage monetization
- 🌙 **Adaptive Theme** — Follows system dark/light mode

## Project Structure

```
app/
├── engine/            # CommandParser + RuleEngine
├── data/
│   ├── db/            # Room Database + DAOs
│   ├── entities/      # Database entities
│   ├── models/        # Domain models
│   └── repository/    # Data layer
├── services/          # Accessibility + Notification + Foreground
├── ui/
│   ├── screens/       # All Compose screens
│   ├── components/    # Reusable UI components
│   └── theme/         # Colors, Typography, Theme
├── viewmodel/         # ViewModels
├── navigation/        # Nav graph
└── util/              # Helpers
```

## Building

### Debug APK
```bash
./gradlew assembleDebug
```

### Release APK
```bash
./gradlew assembleRelease
```

### Play Store AAB
```bash
./gradlew bundleRelease
```

## GitHub Actions Secrets Required

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

### Generate & Encode Keystore
```bash
keytool -genkey -v -keystore taskpulse.jks -keyalg RSA -keysize 2048 -validity 10000 -alias taskpulse
base64 -i taskpulse.jks | pbcopy   # macOS — paste into KEYSTORE_BASE64 secret
```

## Permissions Required

| Permission | Purpose |
|------------|---------|
| Usage Access | Track app usage time |
| Accessibility Service | Detect app launches, show block overlay |
| Notification Listener | Intercept & mute notifications |
| System Alert Window | Show block overlay |

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Database**: Room + SQLite
- **Architecture**: MVVM + Repository
- **DI**: Manual (no Hilt – keeps APK lean)
- **Background**: Foreground Service + BroadcastReceiver

## Developer: chAs Technologies LLC
- Package: `com.chastechgroup.taskpulse`
- Brand: chAs
- Company: chAs Technologies LLC

---

*TaskPulse — Your phone, your rules.*
