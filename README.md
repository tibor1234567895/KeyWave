# KeyWave

Control your music with volume keys when your screen is off.

## What it does

I got tired of having to pull out my phone and unlock it just to skip a song, so I built this. KeyWave lets you control music playback using your volume keys even when the screen is off.

Long press volume up to skip to the next track, long press volume down for previous track, or long press both keys together to play/pause. Regular short presses still work normally for volume control.

## Features

- **Screen-off controls** - Works when your phone screen is off
- **Next track** - Long press volume up
- **Previous track** - Long press volume down  
- **Play/pause** - Long press both volume keys
- **Normal volume** - Short presses work as usual
- **Customizable timing** - Adjust how long you need to hold the keys
- **Haptic feedback** - Optional vibration when a command is detected
- **Battery efficient** - Runs in background without draining your battery
- **Works with most music apps** - Uses standard Android media controls

## How it works

The app uses Android's Accessibility Service to capture volume key presses globally, then sends media control commands to whatever music app you're using. When it detects a long press, it blocks the normal volume change and sends the music command instead.

## Setup

1. Install the app
2. Grant Accessibility Service permission (the app will walk you through this)
3. Enable the KeyWave service in the app settings
4. Adjust long-press durations if needed
5. Test it out with your music app

## Permissions

- **Accessibility Service** - Required to capture volume key presses when screen is off
- **Foreground Service** - Keeps the service running in background

## Building

Clone the repo and open in Android Studio. Standard Android build process.

```bash
git clone https://github.com/tibor1234567895/KeyWave.git
```

## Screenshots

![Screenshot1](Screenshots/Screenshot1.png)
![Screenshot2](Screenshots/Screenshot2.png)

## Contributing

Bug reports and feature requests welcome. Pull requests too.

## License

[MIT License](LICENSE)