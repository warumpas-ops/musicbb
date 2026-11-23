# MusicBB 🎵

**MusicBB** is a dedicated, retro cyber-terminal music player crafted specifically for the **BlackBerry Q5** (BlackBerry 10 OS 10.3.3.3216, Android Runtime API 18 / Jelly Bean 4.3) with a square 720×720 display and physical QWERTY keyboard.

---

## ✨ Features

- **🎨 Retro Terminal & Cyberdeck Aesthetic:**
  - High-contrast dark palette (`#0D0F11`) with phosphor green (`#A8D8A8`) accents.
  - Real-time animated audio spectrum VU meters bouncing with music.
  - Square 1:1 layout designed natively for the 720×720 BlackBerry screen.

- **🖼️ Native Album Cover Art & One-Tap VU Switch:**
  - Automatically extracts high-resolution embedded ID3 album cover art from `.mp3`, `.flac`, `.m4a`, etc.
  - Automatically checks folders for `cover.jpg` / `folder.jpg`.
  - Tap the album cover (or press **`C`**) to toggle between the **Album Cover View** and the **Audio Spectrum Visualizer**!
  - Retro cassette tape fallback for audio files without artwork.

- **🔥 Listening Analytics & "Most Played" Tracker:**
  - Automatically logs every completed listen in an offline local SQLite database.
  - Tracks **Total Plays**, **Total Hours Listened**, and displays an all-time **Top Replayed Tracks Leaderboard**.
  - Now Playing screen badges your replay count (e.g. `🔥 48 PLAYS`).

- **🎶 Playlists System:**
  - **Smart Auto-Playlists:**
    - 🔥 **Most Played** (auto-generated top 25 tracks by play count)
    - ★ **Favorites** (one-tap favorite toggle on any track)
    - 🕒 **Recently Played** (last 50 played tracks)
  - **Custom Playlists:** Create and manage playlists (`[+ new list]`) directly from your library or Now Playing screen.

- **📁 Instant Folder & File System Browser:**
  - Browse `/sdcard/Music/` or your MicroSD card directly with mini album thumbnails.
  - Quick-search filter (`[F]`) to find songs instantly.

- **⌨️ BlackBerry Q5 Physical Keyboard Shortcuts:**
  - **`Space`**: Play / Pause
  - **`N`** / **`P`**: Next / Previous track
  - **`J`** / **`L`**: Seek -5s / +5s
  - **`C`**: Toggle Cover Art vs. Spectrum VU visualizer
  - **`S`**: Toggle Shuffle
  - **`R`**: Toggle Repeat (All / One / Off)
  - **`F`**: Quick-search filter
  - **`1`**: Now Playing Tab
  - **`2`**: Folders / Library Tab
  - **`3`**: Playlists Tab
  - **`4`**: Stats & Leaderboard Tab

- **🔋 Low Resource & Background Playback:**
  - Android Foreground Service keeps music playing seamlessly when screen is locked or app is minimized.
  - Lightweight APK (~60 KB) with minimal memory footprint.

---

## 📥 Installation

1. Copy `MusicBB-v1.0.0.apk` to your BlackBerry device.
2. Open the File Manager and tap the APK to install.
3. Put your music files in `/sdcard/Music/`.
4. Launch MusicBB and enjoy!
