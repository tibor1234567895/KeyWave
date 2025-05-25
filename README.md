# KeyWave: Screen-Off Music Control

## 🎶 Control Your Music, Even When the Screen is Off.

KeyWave is an innovative Android application designed to give you seamless control over your music playback using only your device's hardware volume keys, even when the screen is turned off. No more fumbling for your phone in your pocket or unlocking it just to skip a track!

## ✨ Features

KeyWave empowers you with intuitive long-press gestures on your volume keys for essential media controls:

*   **Screen-Off Operation:** Reliably detects when your device screen is off, enabling the special controls.
*   **Next Track:** A **long press on Volume Up** (`⬆️`) skips directly to the next song in your playlist.
*   **Previous Track / Restart:** A **long press on Volume Down** (`⬇️`) navigates to the previous track, or restarts the current one depending on your media player's default behavior.
*   **Play/Pause Toggle:** **Simultaneously long-press both Volume Up & Down** (`⬆️⬇️`) to instantly toggle playback (Play/Pause) for your current media session.
*   **Normal Volume Control:** Short presses on Volume Up/Down will *always* function as standard volume adjustments, ensuring your device's native functionality remains intact.
*   **Smart Volume Suppression:** When a long-press gesture is successfully detected and a music control action is executed, the default volume adjustment associated with that key press is automatically suppressed.
*   **Customizable Durations:**
    *   **Dedicated Settings Screen:** Access a user-friendly settings screen within the app.
    *   **Per-Action Thresholds:** Independently configure the long-press duration (in milliseconds) for Next Track, Previous Track, and Play/Pause actions.
    *   **Flexible UI Controls:** Adjust durations effortlessly using intuitive **sliders** (e.g., 200ms - 2000ms) or by entering precise values in **text input boxes**.
*   **Haptic Feedback:** Optional vibration feedback confirms successful detection and execution of a long-press music control command.
*   **Service Status Indication:** A clear indicator (e.g., a persistent notification if using a Foreground Service) shows when KeyWave's background service is active and ready to control your music.
*   **Wide Media Player Compatibility:** Utilizes standard Android Media Control APIs (`MediaSessionCompat`) for broad compatibility with most popular music and media player applications.
*   **Resource Efficient:** Designed to minimize battery consumption while running in the background.

## ⚙️ How It Works (Technical Insight)

KeyWave achieves its core functionality by:

*   **Key Event Interception:** Leveraging Android's **Accessibility Service** capabilities (or potentially a persistent Foreground Service) to reliably capture global hardware key events even when the app is not in the foreground or the screen is off.
*   **MediaSession Integration:** Communicating with active media players by sending standard media key events or transport control commands via `MediaSessionCompat`.
*   **Robust Event Handling:** Implementing custom logic to differentiate between short and long presses, manage concurrent key presses, and suppress default system volume actions when a control gesture is detected.
*   **Error Reporting:** Utilizing Android's `Toast` messages for user-facing errors and `Logcat` for internal debugging and detailed error logging.

## 🔒 Permissions

KeyWave requires specific permissions to function correctly:

*   **Accessibility Service:**
    *   `BIND_ACCESSIBILITY_SERVICE`
    *   This is the primary permission needed for KeyWave to intercept global hardware key events when the screen is off. The app will guide you through granting this permission in your device's Accessibility settings.
*   **Foreground Service (potential):**
    *   `FOREGROUND_SERVICE`
    *   If implemented as a Foreground Service, this permission ensures the service runs reliably in the background without being killed by the system.

## 🚀 Getting Started

To get KeyWave up and running on your device:

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/YourUsername/KeyWave.git
    ```
    (Replace `YourUsername` with your actual GitHub username)
2.  **Open in Android Studio:** Open the cloned `KeyWave` project in Android Studio.
3.  **Build and Run:** Build the project and install the APK on your Android device or emulator.
4.  **Grant Permissions:** Upon first launch, the app will guide you to enable the necessary **Accessibility Service** permission in your device settings.
5.  **Enable Service:** Ensure the KeyWave service is enabled within the app's own settings.
6.  **Customize (Optional):** Adjust the long-press durations for each action in the app's settings.
7.  **Enjoy!** Turn off your screen and try controlling your music using the volume keys.

## 📸 Screenshots
![Screenshot1](Screenshots/Screenshot1.png)
![Screenshot2](Screenshots/Screenshot2.png)

## 💡 Contribution

Feel free to open issues for bug reports or feature requests. Pull requests are also welcome!

## 📜 License

This project is licensed under the [MIT License](LICENSE) - see the `LICENSE` file for details.
*(Note: Create a `LICENSE` file in your repository if you choose an open-source license like MIT)*

---