# Product Requirements Document (PRD): KeyWave

**Document Owner:** Development Team  
**Last Updated:** December 27, 2025  
**Version:** 1.0 (MVP)  
**Status:** Draft

---

## 1. Product Overview

**Product Name:** KeyWave  
**Platform:** Android (Primary), potentially customizable for AOSP forks  
**License:** Open Source (GPLv3 or Apache 2.0)  
**Minimum SDK:** Android 8.0 (Oreo)  
**Target SDK:** Latest Android Version (Android 14/15)

### 1.1 Description

KeyWave is an open-source Android application designed to replicate and extend the functionality of the "Volumee" app. Its primary purpose is to allow users to control media playback (skipping tracks, pausing, etc.) using physical volume buttons, even when the screen is off or the device is in a pocket. KeyWave distinguishes itself by offering all features—including those paywalled in competitors—completely free of charge, with a strong commitment to user privacy.

### 1.2 Value Proposition

* **Fully Free:** No paywalls, subscriptions, or "Pro" versions. All advanced features are unlocked by default.
* **Privacy First:** No internet permission required. No data collection. No analytics. No backend server.
* **Open Source:** Transparent codebase allowing community audits and contributions. Reproducible builds.
* **Accessibility:** Enhances device usability for visually impaired users or those operating the device blindly (e.g., while driving or exercising).
* **Community-Driven:** Public issue tracker, welcoming contributions, clear governance.

### 1.3 Product Goals

**Must-Have Outcomes:**
1. **Global volume-button gestures → media actions** (next/previous/play-pause), with **normal volume behavior preserved** when KeyWave shouldn't intercept
2. Works **screen ON and OFF** (with clear guidance for platform limitations, e.g., Android 12+ behavior)
3. Works across **popular media apps** by targeting the **currently active** player/session
4. Includes Volumee "PRO" features for free: Quick Settings tile, custom click actions, choose supported players, action notifications
5. **No dedicated backend** and **no personal data collection**

**Non-Goals:**
- Building a music player/streaming service
- Root-only hooking / OEM-specific hacks as primary path
- Cloud accounts, telemetry, or advertising

---

## 2. User Personas & Stories

### Primary Personas

**"Pocket Listener"**
- Walking/driving/working out; wants to skip/pause without unlocking phone
- **Story:** *As a commuter, I want to skip the current song by holding the "Volume Up" button while my phone is in my pocket so that I don't have to take my phone out.*

**"Accessibility User"**
- Visually impaired or has broken screen digitizer; needs hands-free control
- **Story:** *As a user with a broken screen digitizer, I want to control media playback using physical keys.*

**"Privacy Advocate"**
- Wants full control over data
- **Story:** *As a privacy advocate, I want an app that performs these actions without sending usage data to a third-party server.*

**"Power User"**
- Wants extensive customization
- **Story:** *As a power user, I want to customize exactly how long I need to hold the button and what vibration feedback I get to confirm the action.*

**"Spotify Enthusiast"**
- **Story:** *As a Spotify user, I want to "Like" the currently playing song using a hardware button shortcut.*

---

## 3. Functional Requirements

### 3.1 Core Interaction (The "Engine")

The app must utilize the **Android Accessibility Service API** to intercept physical key events.

| ID | Feature | Description | Priority |
|---|---|---|---|
| **F-01** | **Button Interception** | Detect "Volume Up" and "Volume Down" key presses globally | P0 |
| **F-02** | **Long Press Trigger** | Execute an action when a volume button is held for a user-defined duration | P0 |
| **F-03** | **Screen State Support** | Functionality must work when the screen is **ON**, **OFF**, and **LOCKED**. *(Note: Android 12+ requires specific AOD handling)* | P0 |
| **F-04** | **Event Passthrough** | If the trigger condition (long press) is *not* met, the default volume change action must occur without delay | P0 |
| **F-05** | **Gesture Detection Latency** | Feels instant (< 150ms perceived delay) | P0 |

### 3.2 Action Mapping (Standard & "Pro" Unlocked)

KeyWave must support all actions found in the competitor's paid tier.

| ID | Feature | Description | Priority |
|---|---|---|---|
| **F-06** | **Media Controls** | Map triggers to: Next Track, Previous Track, Play/Pause, Stop | P0 |
| **F-07** | **Active Session Targeting** | Send media commands to the **active media session/player** (not hardcoded app) | P0 |
| **F-08** | **Custom Actions** | Map triggers to: Mute/Unmute, Launch specific app, Toggle Flashlight, Google Assistant | P1 |
| **F-09** | **Spotify Integration** | Map a trigger to "Add to Favorites/Like" for the current Spotify track | P2 |
| **F-10** | **Sensitivity Config** | Slider to adjust "Long Press Duration" (e.g., 200ms to 2000ms) | P1 |
| **F-11** | **Haptic Feedback** | Customizable vibration patterns (Off, Short, Long, Double, custom intensity + pulses) upon successful trigger execution | P1 |
| **F-12** | **Advanced Gesture Mapping** | Support for double-press (optional), long-press both buttons (Play/Pause) | P2 |

### 3.3 App Management & Context

| ID | Feature | Description | Priority |
|---|---|---|---|
| **F-13** | **App Allowlist/Blocklist** | Users can select specific media apps (e.g., Spotify, YouTube) where KeyWave is active. By default, auto-detect active media sessions | P1 |
| **F-14** | **Quick Settings Tile** | A toggle in the Android Quick Settings panel to quickly enable/disable KeyWave service (with states: ON, OFF, Error) | P1 |
| **F-15** | **Pocket Mode** | Option to only enable triggers when the proximity sensor is covered (preventing accidental presses when using the phone normally) | P2 |
| **F-16** | **Smart Activation** | Only intercept when: A recognized media player is active/playing OR showing a media notification (configurable) | P1 |
| **F-17** | **Activation Rules** | Configure when KeyWave should be active (e.g., always, only when media playing, schedule/time window) | P2 |
| **F-18** | **Action Notifications** | Optional user-visible feedback: Toast/snackbar, Heads-up notification (optional, off by default) | P1 |

### 3.4 Diagnostics & Supportability

| ID | Feature | Description | Priority |
|---|---|---|---|
| **F-19** | **Status Screen** | Display: permissions OK, service running, last detected player, last action | P1 |
| **F-20** | **Debug Logging** | Optional debug logging/export (local only, opt-in) | P2 |
| **F-21** | **Known Issues List** | Device-specific compatibility notes | P2 |

---

## 4. User Experience (UI/UX)

### 4.1 Design System

* **Design System:** Material You (Material 3) with dynamic color support
* **Dark Mode:** Full support with automatic theme switching
* **Accessibility:** High contrast, large touch targets, screen reader compatible

### 4.2 Onboarding Flow

1. **Welcome Screen:** Explanation of KeyWave's purpose (short and clear)
2. **Permission Checklist UI:**
   - **Accessibility Service** (required for global key interception) — with clear explanation of *why* needed
   - **Notification Listener** (to detect active media app/session)
   - Direct buttons to open Settings at correct pages
3. **Battery Optimization:** Prompt to disable battery optimization for KeyWave to ensure consistent background performance
4. **Test Your Setup:** Play music → try gesture → show confirmation

### 4.3 Main Dashboard

* **Big "Service Enabled" Toggle** (prominent ON/OFF switch)
* **Cards for "Volume Up" and "Volume Down" configuration**
  - Action mapping (Next, Previous, Play/Pause, etc.)
  - Long-press duration slider
  - Vibration pattern selector
* **Advanced Settings Section:**
  - Haptics customization (intensity, patterns)
  - App Allowlist/Blocklist
  - Screen On/Off behavior
  - Pocket Mode toggle
  - Activation rules

### 4.4 Quick Control

* Quick Settings tile toggles KeyWave service ON/OFF
* Shows current status (ON/OFF/Error)

---

## 5. Technical Specifications

### 5.1 Architecture

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (preferred) or Views
* **Design Pattern:** MVVM (Model-View-ViewModel)
* **Build System:** Gradle with reproducible builds

### 5.2 Core Components

* **App UI:** Jetpack Compose (or Views), settings + onboarding
* **Core Services:**
  - `AccessibilityService`: intercept volume-key long presses (where permitted)
  - `NotificationListenerService` and/or `MediaSessionManager` callbacks: detect active media and target session
  - Foreground service where required for reliability (with transparent notification and opt-out when possible)

### 5.3 Permissions

* **Accessibility Service:** Required to intercept key events
* **Notification Listener:** Required to detect which media player is active (to target the "Next Track" command correctly)
* **Vibrate:** For haptic feedback
* **Foreground Service:** To keep the listener active without being killed by battery optimization
* **Query All Packages:** Required to populate the list of installed apps for the Allowlist/Blocklist feature

### 5.4 Platform Constraints

#### Android 12+ Screen-Off Behavior
* **Issue:** On Android 12 and newer, the OS restricts background Accessibility events when the screen is fully off to save battery.
* **Solution:** Implement the "Always On Display" (AOD) workaround used by Volumee. KeyWave must detect if the user is on Android 12+ and prompt them to enable AOD if necessary for screen-off functionality.
* **User Guidance:** Clear in-app messaging about platform limitations and required settings.

#### OEM Differences
* Handle OEM-specific behaviors in delivering key events while screen off
* Maintain compatibility matrix for known device issues

### 5.5 Privacy & Security

* **Network Access:** The app manifest must **NOT** include `android.permission.INTERNET`. This guarantees no data exfiltration.
* **Data Storage:** All configuration data (preferences, mapping) must be stored locally (DataStore or SharedPreferences)
* **Notification Content:** Do not read/process notification content beyond what's needed to detect "is media" and "which player"
* **Clear Disclosures:** Transparent explanations for Accessibility + Notification permissions
* **Privacy Policy:** Publish clear policy mirroring "no data collection" commitment

---

## 6. Non-Functional Requirements

### 6.1 Performance

* **Gesture Detection Latency:** Feels instant (< 150ms perceived delay)
* **Battery Impact:** Minimal wakeups; avoid constant polling
* **Memory Footprint:** Lightweight service with minimal RAM usage
* **App Launch Time:** < 1 second on mid-range devices

### 6.2 Reliability

* **Crash-Free Sessions:** Target 99.5%+
* **Action Success Rate:** > 95% when media is active
* **Service Stability:** No unexpected stops or crashes

### 6.3 Compatibility

* **Minimum SDK:** Android 8.0 (Oreo)
* **Target SDK:** Latest Android Version (Android 14/15)
* **Device Coverage:** Support major OEMs (Samsung, Google, OnePlus, Xiaomi, etc.)
* **Graceful Degradation:** Handle Android 12+ screen-off constraints with clear user guidance

### 6.4 Maintainability

* **Code Quality:** Lint-free, documented, tested
* **Test Coverage:** Unit tests for business logic, UI tests for critical flows
* **CI/CD:** Automated builds, tests, and lint checks

---

## 7. Open-Source & Distribution

### 7.1 Repository & License

* **License Options:** 
  - GPLv3 (strong copyleft, ensures derivatives stay open)
  - Apache 2.0 (permissive, allows commercial use)
* **Repository Structure:**
  - CONTRIBUTING.md (contribution guidelines)
  - CODE_OF_CONDUCT.md (community standards)
  - SECURITY.md (security policy and reporting)
  - LICENSE (chosen license text)
  - README.md (clear setup and build instructions)

### 7.2 Reproducible Builds

* Deterministic build process
* Clear build instructions in documentation
* Version tagging and release notes

### 7.3 Distribution Channels

* **GitHub Releases:** APK downloads with signatures
* **F-Droid:** Strong alignment with "free + open source" (recommended)
* **Google Play:** Optional (requires careful policy compliance around Accessibility use)

### 7.4 Community Engagement

* **Public Issue Tracker:** GitHub Issues with labels ("good first issue", "bug", "enhancement")
* **Automated CI:** Unit tests + lint on every PR
* **Code Reviews:** Required for all contributions
* **Documentation:** Developer docs, API docs, user guides

---

## 8. Comparison: Volumee vs. KeyWave

| Feature | Volumee (Free) | Volumee (Paid) | KeyWave (Open Source) |
|---|---|---|---|
| Skip Tracks | ✓ | ✓ | **✓** |
| Screen Off Support | ✓ | ✓ | **✓** |
| Custom Vibration | Limited | Full | **Full** |
| App Allowlist/Blocklist | ✗ | ✓ | **✓** |
| Custom Actions | ✗ | ✓ | **✓** |
| Quick Settings Tile | ✗ | ✓ | **✓** |
| Action Notifications | ✗ | ✓ | **✓** |
| Spotify Integration | ✗ | ✓ | **✓** |
| Privacy (No Internet) | Good | Good | **Best (Guaranteed)** |
| Open Source | ✗ | ✗ | **✓** |
| Cost | Free | $2-5 | **Free** |

---

## 9. Success Metrics

### 9.1 Setup Success

* **Setup Completion Rate:** % of users who grant all required permissions
* **Permission Grant Rate:** % of users who grant each permission
* **Onboarding Drop-off:** Track where users abandon setup

### 9.2 Feature Usage

* **Action Success Rate:** % of gestures that successfully trigger media actions (target: > 95%)
* **Daily Active Service:** % of installs with service enabled
* **Feature Adoption:** % of users customizing actions, using Quick Settings tile, etc.

### 9.3 Quality

* **Crash-Free Sessions:** Target 99.5%+
* **User-Reported Issues:** Track by device model (opt-in bug reports only)
* **GitHub Issues:** Response time, resolution time

### 9.4 Community

* **GitHub Stars/Forks:** Measure community interest
* **Contributions:** Number of external contributors, PRs
* **F-Droid Downloads:** Track adoption in privacy-conscious community

---

## 10. Roadmap

### Phase 1: MVP (Minimum Viable Product)

**Timeline:** 4-6 weeks

* Core accessibility service implementation
* Volume Up/Down long-press mapping to Next/Previous track
* Screen On/Off support
* Basic UI for toggling service
* Permission onboarding flow
* Settings: long-press threshold + vibration toggle

**Acceptance Criteria:**
- Long-press up/down works for next/previous
- Smart activation prevents interference when no media is active
- Settings: long-press threshold + vibration toggle
- Permission onboarding complete and understandable

### Phase 2: "Pro" Feature Parity

**Timeline:** 6-8 weeks

* App Allowlist/Blocklist
* Custom click actions mapping
* Vibration customization (patterns, intensity)
* Quick Settings Tile (ON/OFF/Error states)
* Action notifications
* Dark Mode / Material You theming
* Status screen with diagnostics

**Acceptance Criteria:**
- Quick Settings tile functional
- Custom click actions mapping complete
- Supported players allowlist/blocklist works
- Action notifications customizable
- Clear Android 12+ screen-off guidance provided

### Phase 3: Innovation (Better than Original)

**Timeline:** 8+ weeks

* **Short Press Override:** Ability to remap single clicks (experimental)
* **Tasker Integration:** Broadcast intents so Tasker/MacroDroid can react to KeyWave triggers
* **Flashlight Control:** Toggle flashlight with volume long-press
* **Pocket Mode:** Proximity sensor integration
* **Advanced Gestures:** Double-press, combination presses
* **Spotify "Like" Feature:** Direct integration

---

## 11. Risks & Mitigations

### 11.1 Policy Risk (Accessibility Misuse Perception)

**Risk:** Google Play may flag accessibility service as potential misuse  
**Severity:** High  
**Mitigation:**
- Strong in-app disclosure explaining exactly why permissions are needed
- Strict scope: no unrelated data access
- Privacy policy clearly stating no data collection
- Consider F-Droid as primary distribution if Play Store issues arise

### 11.2 Device/OEM Inconsistencies

**Risk:** App may not work consistently across all Android devices  
**Severity:** Medium  
**Mitigation:**
- Diagnostics screen showing current state
- Known-issues list with device-specific workarounds
- Community device compatibility matrix
- Clear user guidance for common issues

### 11.3 Android Platform Changes

**Risk:** Future Android versions may restrict accessibility/notification access  
**Severity:** Medium  
**Mitigation:**
- Stay updated on Android beta releases
- Maintain compatibility with last 3-4 Android versions
- Document platform-specific limitations clearly
- Explore alternative APIs proactively

### 11.4 IP / Branding / "Clone" Concerns

**Risk:** Perception as a "clone" of proprietary app  
**Severity:** Low  
**Mitigation:**
- Do not copy Volumee branding/UI assets
- Implement functionality independently (clean-room)
- Choose original name/iconography (KeyWave)
- Document independent development intent
- Focus on open-source value proposition

### 11.5 Battery Optimization Interference

**Risk:** Android battery optimization may kill background service  
**Severity:** Medium  
**Mitigation:**
- Clear onboarding guidance to disable optimization
- Foreground service with user-visible notification
- Periodic service health checks
- User-friendly restart mechanisms

### 11.6 Maintenance & Sustainability

**Risk:** Project may become unmaintained without community support  
**Severity:** Medium  
**Mitigation:**
- Clear contribution guidelines
- Good first issue labels
- Welcoming community culture
- Multiple maintainers if possible
- Transparent governance model

---

## 12. Technical Challenges

### 12.1 Key Technical Challenges

1. **OEM Differences:** Delivering key events while screen off varies by manufacturer
2. **Android 12+ Constraints:** Platform restrictions on background accessibility (AOD workaround)
3. **Avoiding "Sticky" Interference:** Must not interfere with normal volume behavior
4. **Battery Efficiency:** Minimize wakeups while maintaining responsiveness
5. **Session Detection:** Reliably identifying the active media session across apps

### 12.2 Proposed Solutions

* Use `AccessibilityService` for global key event interception
* Use `NotificationListenerService` + `MediaSessionManager` for session detection
* Implement smart debouncing to distinguish long-press from short-press
* Use foreground service with low-priority notification for reliability
* Detect Android version and guide users through platform-specific setup

---

## 13. Acceptance Criteria

### 13.1 MVP Acceptance (Minimum Shippable)

- [ ] Long-press up/down works for next/previous
- [ ] Works with screen on and off (with guidance for Android 12+)
- [ ] Smart activation prevents interference when no media is active
- [ ] Settings: long-press threshold + vibration toggle
- [ ] Permission onboarding complete and understandable
- [ ] Quick Settings tile functional
- [ ] Battery optimization guidance provided

### 13.2 Full Parity Acceptance (Volumee + Paywalled Features)

- [ ] Quick Settings tile with ON/OFF/Error states
- [ ] Custom click actions mapping
- [ ] Supported players allowlist/blocklist
- [ ] Action notifications (customizable)
- [ ] Vibration patterns (multiple options)
- [ ] Clear Android 12+ screen-off guidance
- [ ] Status/diagnostics screen
- [ ] Material You theming

### 13.3 Quality Gates

- [ ] Crash-free rate > 99%
- [ ] Action success rate > 95%
- [ ] Setup completion rate > 80%
- [ ] No critical bugs in issue tracker
- [ ] All P0 features implemented and tested

---

## 14. Privacy Policy Summary

KeyWave is committed to user privacy:

* **No Internet Access:** App does not have internet permission and cannot send data anywhere
* **No Data Collection:** We do not collect, store, or transmit any user data
* **Local Storage Only:** All settings stored locally on your device
* **Minimal Permissions:** Only requests permissions essential for functionality:
  - Accessibility: To detect volume button presses
  - Notification Listener: To detect active media apps
  - Vibrate: For haptic feedback
* **Transparent:** Full source code available for audit
* **No Third Parties:** No analytics, no ads, no tracking

---

## 15. Appendix

### 15.1 References

* Android Accessibility Service API documentation
* Android MediaSession API documentation
* Material Design 3 guidelines
* Volumee app analysis (functional, not source)

### 15.2 Glossary

* **AOD:** Always On Display
* **OEM:** Original Equipment Manufacturer (device maker)
* **MVP:** Minimum Viable Product
* **P0/P1/P2:** Priority levels (0=Critical, 1=Important, 2=Nice-to-have)
* **PRD:** Product Requirements Document

### 15.3 Document History

| Version | Date | Changes | Author |
|---|---|---|---|
| 1.0 | 2025-12-27 | Initial combined PRD | Development Team |

---

**End of Document**
