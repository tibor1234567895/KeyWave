# KeyWave

Control your music with hardware buttons, even when your screen is off.

KeyWave is an open-source Android app that maps volume button gestures to media actions. Skip tracks, pause playback, or trigger other actions without ever touching your screen. It's designed for commuters, runners, drivers, or anyone who wants quick, tactile control over their music.

---

## Features

**Core Functionality**
- Long-press volume buttons to skip tracks, play/pause, or stop playback
- Works with the screen on, off, or locked
- Targets the currently active media session (Spotify, YouTube Music, Podcasts, etc.)
- Normal volume behavior preserved when gestures aren't triggered

**Customization**
- Adjustable long-press duration (200ms to 2000ms)
- Configurable haptic feedback patterns (off, short, long, double, or custom)
- Custom keybinds for more complex button sequences
- App allowlist/blocklist to control which media apps trigger KeyWave

**Additional Actions**
- Next / Previous track
- Play / Pause / Stop
- Mute toggle
- Flashlight toggle
- Launch Google Assistant
- Add current track to Spotify favorites

**Activation Rules**
- Always active, or only when media is playing
- Screen state filtering (any, screen on only, screen off only)

---

## Requirements

- Android 8.0 (Oreo) or higher
- Accessibility Service permission (to detect volume button presses)
- Notification Listener permission (to identify the active media session)

---

## Installation

### From Releases
1. Download the latest APK from the [Releases](../../releases) page
2. Enable installation from unknown sources if prompted
3. Install and open the app
4. Follow the onboarding flow to grant required permissions

### Building from Source
```bash
git clone https://github.com/tibor1234567895/KeyWave.git
cd KeyWave
./gradlew assembleDebug
```
The APK will be in `app/build/outputs/apk/debug/`.

---

## Permissions

KeyWave requests only the permissions necessary for its functionality:

| Permission | Purpose |
|------------|---------|
| Accessibility Service | Intercept volume button presses globally |
| Notification Listener | Detect which media app is currently active |
| Vibrate | Provide haptic feedback on successful actions |
| Foreground Service | Keep the service running reliably in the background |

KeyWave does not request the `INTERNET` permission and cannot send data anywhere.

---

## Privacy

KeyWave is built with privacy as a core principle:

- No data collection whatsoever
- No analytics, ads, or tracking
- No network access (no internet permission in the manifest)
- All settings stored locally on your device
- Full source code available for audit

See [PRIVACY.md](PRIVACY.md) for more details.

---

## Android 12+ Note

Starting with Android 12, the OS restricts background accessibility events when the screen is fully off. If you're experiencing issues with screen-off functionality, you may need to enable Always On Display (AOD) in your device settings. The app will guide you through this during setup if needed.

---

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

If you find a bug or have a feature request, please open an issue. When reporting bugs, include your device model and Android version.

---

## License

KeyWave is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for the full text.

---

## Acknowledgments

KeyWave was built as a free, open-source alternative to paid media control apps. The goal is to provide all the functionality users need without paywalls, subscriptions, or privacy compromises.
