# Moztro Client (Android)

<div align="center">

![Moztro Icon](https://raw.githubusercontent.com/measureofsuccess-studio/moztro-server/main/src/renderer/icon.png)

**Universal PC-Android Seamless Bridge**  
*Next-generation local mobile control center & high-performance productivity client.*

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/measureofsuccess-studio/moztro-client/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Compose-lightgrey.svg)](https://github.com/measureofsuccess-studio/moztro-client)
[![Organization](https://img.shields.io/badge/org-Measure%20of%20Success-black.svg)](https://github.com/measureofsuccess-studio)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

[**Download Android APK (.apk)**](https://github.com/measureofsuccess-studio/moztro-client/releases/latest) • [**PC Server Repository 🖥️**](https://github.com/measureofsuccess-studio/moztro-server)

</div>

---

## 📱 Overview

**Moztro Client** is a native Android application built with modern **Kotlin** and **Jetpack Compose**. It transforms your smartphone into a versatile, high-precision remote suite and secondary display for your Windows PC over local Wi-Fi / Hotspot without relying on any external cloud or internet connection.

---

## ✨ Features

- ⚡ **Overdrive Screen Mirroring**: Low-latency H.264 / JPEG real-time display streaming from Windows PC directly to Android.
- 🎯 **Dual Input Modes**: Direct Touch (tap/drag directly on the mirrored screen) and Precision Trackpad mode (relative cursor movement with hardware cursor sync).
- 🔊 **Live PCM Audio Stream**: Real-time audio streaming from PC to phone with low-latency playback.
- 📂 **Fast File Sharing**: Select single or multiple files & photos directly from the native Android file picker and transfer them wirelessly at high speeds.
- 🗄️ **View On Device (VOD)**: One-tap FTP server enabling Windows File Explorer to browse and manage the entire Android storage without cables.
- ⌨️ **Remote Input & Power Controls**: Custom split touchpad/keyboard, full Windows modifier combos (Ctrl, Alt, Shift, Win), and remote power controls (Shutdown, Restart, Lock).
- 🖤 **Monochrome Industrial Design**: Clean, high-contrast dark theme engineered for rapid responsiveness and battery efficiency.

---

## 🖥️ Ecosystem Companion

Moztro Client communicates over local WebSockets and HTTP with the **Moztro PC Server**:
👉 [**Explore Moztro Server Repository (PC)**](https://github.com/measureofsuccess-studio/moztro-server)

---

## 📥 Installation

1. Download the latest **`Moztro-1.0.0.apk`** from the [**Releases Page**](https://github.com/measureofsuccess-studio/moztro-client/releases/latest).
2. Install the APK on your Android device (Android 8.0+ / API 26+).
3. Connect your Android device and PC to the same Wi-Fi network or mobile hotspot.

---

## 🛠️ Building from Source

### Prerequisites
- [Android Studio Ladybug](https://developer.android.com/studio) or newer
- JDK 17
- Android SDK (API 35)

### Build APK
```bash
# Clone the repository
git clone https://github.com/measureofsuccess-studio/moztro-client.git
cd moztro-client

# Build debug APK
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👨‍💻 Developer & Creator

- **Developer**: Akbar Dwi Mulya
- **Organization**: [Measure of Success](https://github.com/measureofsuccess-studio)
- **Instagram**: [@measureofsuccess.official](https://instagram.com/measureofsuccess.official)
- **Email**: `measureofsuccess.official@gmail.com`

---

## 📄 License

Copyright © 2026 Akbar Dwi Mulya (Measure of Success). All rights reserved.
Licensed under the [MIT License](LICENSE).
