# MusicBB

> **Minimalist Hi-Fi Music Player for BlackBerry 10**  
> Custom-engineered for square 720×720 displays (BlackBerry Q5, Q10, Classic, Passport) on Android API 18 / Jelly Bean 4.3+.

![BlackBerry 10 Compatible](https://img.shields.io/badge/BlackBerry_10-Tested_&_Verified-blue?style=flat-square)
![Release](https://img.shields.io/badge/Release-v2.2.0-white?style=flat-square)
![Display](https://img.shields.io/badge/Display-720x720_1:1-black?style=flat-square)
![Font](https://img.shields.io/badge/Font-Space_Grotesk-emerald?style=flat-square)

---

## Overview

**MusicBB** is a dedicated offline music player designed from the ground up to respect the unique hardware of square BlackBerry devices. Built with a pure Android API 18 runtime, zero heavy frameworks, and zero cloud tracking, it delivers a tactile, high-contrast listening experience that feels like a piece of vintage audio hardware in your pocket.

---

## Design Philosophy

- **OLED True Black & Titanium Palette**: Pure `#090A0D` background paired with titanium whites (`#F4F4F6`), muted slate (`#71717A`), and warm amber accents (`#F59E0B`). No harsh phosphor green, no eye fatigue.
- **Space Grotesk Typography**: The geometric typeface applied across every screen, dialog, track title, and list row via recursive font binding.
- **Permanent Album Artwork & Hi-Fi Vinyl Fallback**: Continuous hero album cover display with smooth rounded borders. For tracks without embedded ID3 artwork, MusicBB generates a procedural rotating vinyl record with the track metadata printed right on the center label.
- **Zero-Emoji Custom Vector Icons**: Standard Android emoji fonts substitute media controls with colorful blue boxes on older Jelly Bean runtimes. MusicBB uses custom anti-aliased vector PNG drawables for shuffle, previous, play, pause, next, and repeat.
- **Instant SQLite Analytics**: Automatically tracks total plays, listening hours, and favorite songs completely offline.

---

## BlackBerry Physical Keyboard Shortcuts

Navigate and control playback without touching the screen:

| Key | Action |
|:---:|:---|
| <kbd>Space</kbd> | Play / Pause |
| <kbd>N</kbd> | Next Track |
| <kbd>P</kbd> | Previous Track |
| <kbd>J</kbd> / <kbd>L</kbd> | Seek Rewind 5s / Fast-Forward 5s |
| <kbd>S</kbd> | Toggle Shuffle |
| <kbd>R</kbd> | Cycle Repeat Mode (Repeat All → Repeat One `1x` → Off) |
| <kbd>C</kbd> | Toggle Favorite on current track (★) |
| <kbd>F</kbd> | Focus Search Filter instantly |
| <kbd>1</kbd> | Switch to **Player** (Now Playing) |
| <kbd>2</kbd> | Switch to **Tracks** (Library Browser) |
| <kbd>3</kbd> | Switch to **Playlists** (Smart & Custom Lists) |
| <kbd>4</kbd> | Switch to **Stats** (Listening Analytics & Top Replayed) |

---

## App Screens

### 1. Player (Now Playing)
- 160×160 hero album art frame with procedural vinyl fallback.
- Marquee scrolling track title and artist/album metadata.
- Dynamic bitrate & replay badge (`320 KBPS · 14 PLAYS`).
- Tactile transport controls (tactile white center pill for play/pause, dedicated shuffle & repeat toggles).
- One-tap `★ FAVORITE` and `+ PLAYLIST` actions.

### 2. Tracks (Library)
- High-speed local media scanner for internal storage (`/sdcard/Music`) and MicroSD cards.
- Live real-time search filter with instant keyboard typing.
- Mini album thumbnail art with track duration and play counts.

### 3. Playlists
- **Top Played**: Automatically ranked by offline play count.
- **Favorites**: All starred tracks ready for instant playback.
- **Recently Played**: Last 50 tracks queued chronologically.
- **Custom Playlists**: Create, rename, and manage playlists directly on the device.

### 4. Stats & Analytics
- Total lifetime plays counter.
- Total streamed hours logged locally.
- Most-played tracks leaderboard.

---

## Sideloading onto BlackBerry 10

1. Download **`MusicBB-v2.2.0.apk`** from the [GitHub Releases](https://github.com/warumpas-ops/musicbb/releases) page.
2. Transfer the `.apk` to your BlackBerry device via USB cable, MicroSD card, or download directly in the BlackBerry browser.
3. Open the native **File Manager** app on your BlackBerry, navigate to the APK, and tap **Install**.
4. The APK is signed with **v1 JAR signing** scheme for direct native installation on BB10 (OS 10.3.x).
5. Put your music files in `/sdcard/Music/` or any folder on your SD card, open MusicBB, tap **SCAN**, and start listening.

---

## Building from Source

### Prerequisites
- JDK 17
- Android SDK (target API 18, build-tools 34.0.0+)
- Gradle 8.4+

```bash
# Clone the repository
git clone https://github.com/warumpas-ops/musicbb.git
cd musicbb

# Build the signed release APK
gradle assembleRelease

# Output APK path:
# app/build/outputs/apk/release/app-release.apk
```

---

## License

Crafted for the BlackBerry community under the [MIT License](LICENSE).
