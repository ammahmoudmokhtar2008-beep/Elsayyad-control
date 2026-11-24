# Elsayyad Control

Android voice-control assistant prototype built with Kotlin and Jetpack Compose.

## Current baseline
- Arabic (Egypt) speech recognition via Android SpeechRecognizer.
- User-started foreground microphone service with persistent notification.
- Basic commands: volume up/down/mute, Back, Home, Settings, launch YouTube/WhatsApp, and open dialer.
- Accessibility service scaffold for global navigation actions.
- Arabic-first dark UI.

## Build
Open the project in Android Studio (Giraffe or newer) with Android SDK 35 and JDK 17, or run:

```bash
gradle assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and platform behavior
The microphone permission must be granted and the user must start the listening service while the app is visible. Android requires a persistent foreground-service notification for microphone use. Some Android versions and manufacturers may stop services under battery restrictions. Accessibility must be enabled manually in system settings for Back/Home actions.

Calls are opened in the system dialer; the app does not place calls automatically. Protected lock-screen and security actions cannot be bypassed. Gesture recognition and contact-name resolution are not implemented in this baseline yet.

## Roadmap
1. Reliable command parsing and confirmation flows.
2. Contacts lookup and explicit call confirmation.
3. Camera gesture engine with opt-in foreground camera service.
4. Automated device testing and crash reporting integration.
