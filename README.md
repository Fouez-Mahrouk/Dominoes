# Dominoes

A clean, offline, ad-free dominoes game. No tracking. No data collection. No internet required.

## Screenshots

| Light Mode | Dark Mode |
|:---:|:---:|
| ![Light Mode](Screenshots/Game-light.png) | ![Dark Mode](Screenshots/Game-dark.png) |

| Start Screen | Scoring |
|:---:|:---:|
| ![Start Screen](Screenshots/Start-screen.png) | ![Scoring](Screenshots/Scoring.png) |

| Settings |
|:---:|
| ![Settings](Screenshots/Settings.png) |

## Features

- **5 game modes:** Free Game, Block, All Fives, All Threes, Fives & Threes
- **2, 3, or 4 players** (human + AI)
- **3 AI difficulty levels:** Easy, Medium, Hard
- **Modern Material 3 UI** with automatic light/dark mode
- **Haptic feedback** — vibration on tile pickup, placement, scoring, and win
- **Sound effects** — subtle, offline, no bloat
- **Auto-pan camera** — follows the action, no zooming out
- **Score hint badges** — shows points per placement while a tile is selected
- **Compact board layout** — tiles cluster around the spinner
- **Save & resume** — close the app, come back, pick up where you left off
- **100% offline** — no internet, no ads, no tracking, no data collection

## Install

**Requires:** Android 8.0+ (API 26)

1. Download [Dominoes.apk](https://github.com/Fouez-Mahrouk/Dominoes/releases) from the latest release
2. Enable **"Install unknown apps"** for your browser (Settings → Security)
3. Open the APK → **Install**

## Game Modes

| Mode | Rules |
|------|-------|
| **Free Game** | Draw from boneyard. First to empty hand wins. Lowest pip count if blocked. |
| **Block** | No drawing. If you can't play, you pass. Lowest pip count wins when blocked. |
| **All Fives** | Score when open ends total a multiple of 5. First to target score wins. |
| **All Threes** | Score when open ends total a multiple of 3. First to target score wins. |
| **Fives & Threes** | Score on multiples of 5 OR 3. First to target score wins. |

## Build from Source

Requires JDK 17 + Android SDK 35.


cd android
./gradlew assembleRelease   

## License

[GPL v3](LICENSE)

## Credits

Built by [Fouez Mahrouk](https://github.com/Fouez-Mahrouk)   