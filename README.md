# Axion Live Wallpaper Picker & 120FPS MP4 Engine
Custom Live Wallpaper Picker and Hardware-Accelerated 120Hz Video Wallpaper Service built for **Infinix Hot 50 4G (`X6882`)** running **AxionOS 2.9 (Android 16)**.

## Features
- **Full Android 16 Package Visibility (`<queries>`)**: Detects all installed `WallpaperService` engines on AOSP / AxionOS.
- **Built-in 120 FPS Local MP4 Video Engine**:
  - Copies selected `.mp4` into internal storage (`files/active_wallpaper.mp4`) for persistent boot survival.
  - Locks `Surface.setFrameRate(120.0f)` for smooth 120Hz playback on the Infinix X6882 display.
  - Automatically pauses playback when another app or game is in the foreground (0% background GPU/CPU drain).
  - Supports **Mute Audio** and **Fullscreen Center-Crop (1080x2460)** live toggles.
- **3-Tier Apply Mechanism**:
  1. Native `ACTION_CHANGE_LIVE_WALLPAPER` preview sheet.
  2. Privileged API fallback (`WallpaperManager.setWallpaperComponent`) when installed as a `priv-app`.
  3. Root shell (`su`) fallback for KernelSU / Magisk users if `LiveWallpapersPicker.apk` is stripped from the ROM.
