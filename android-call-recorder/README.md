# Simple Call Recorder (Android)

A minimal Android app that records phone calls to local audio files and lists
them for playback. Kotlin, single activity, no third-party backend.

## ⚠️ Read this first

**1. Legality.** Recording a phone call without the other party's consent is
illegal in many places (two-party-consent jurisdictions). This app records with
a persistent "Recording call" notification and is intended for your own lawful
use. You are responsible for complying with the laws where you and the other
party are located.

**2. Android limits what you can record.** Since **Android 10 (API 29)**, Google
removed the public API that let third-party apps capture the **remote** party's
voice. On a normal (non-rooted) phone running Android 10+:

- ✅ Your own microphone is recorded reliably.
- ❌ The other person's voice is usually silent or very faint.

Full two-way recording only works on **Android 9 or older**, **some OEM ROMs**
(certain Xiaomi/Samsung/etc. builds expose it), or **rooted devices**. This is
an operating-system restriction — there is no app-level trick around it on stock
modern Android, and this project does not attempt one.

## What it does

- Detects when a call connects (`PHONE_STATE` → `OFFHOOK`) and starts a
  foreground recording service.
- Records to `AAC/.m4a` using `MediaRecorder` with the
  `VOICE_COMMUNICATION` audio source.
- Stops and saves when the call ends (`IDLE`).
- Lists saved recordings in the app; tap one to play it in any media player.

Recordings are stored in the app's private external files dir:
`Android/data/com.example.callrecorder/files/recordings/`.

## Build

You need **Android Studio** (Hedgehog or newer) or the Android SDK + JDK 17.

### Android Studio (easiest)
1. `File → Open` and select the `android-call-recorder/` folder.
2. Let it sync (it will download the Gradle wrapper + dependencies).
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)`.
4. The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Command line
From `android-call-recorder/` (after Android Studio has generated the wrapper,
or run `gradle wrapper` once with a local Gradle 8.9):

```bash
./gradlew assembleDebug
```

> The binary `gradle-wrapper.jar` and `gradlew` scripts are intentionally not
> committed. Opening the project in Android Studio regenerates them, or run
> `gradle wrapper --gradle-version 8.9` with a system Gradle.

## Install

Copy the APK to your phone and enable "Install unknown apps" for your file
manager, or:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

On first launch, tap **Grant permissions** (microphone, phone state,
notifications). Then place or receive a call — recording starts automatically.

## Project layout

```
app/src/main/
├── AndroidManifest.xml          permissions, receiver, service, file provider
├── java/com/example/callrecorder/
│   ├── MainActivity.kt          permissions + recordings list UI
│   ├── CallStateReceiver.kt     starts/stops the service on call state
│   ├── CallRecordingService.kt  foreground MediaRecorder service
│   └── RecordingsAdapter.kt     RecyclerView list + playback intent
└── res/                         layouts, strings, launcher icon
```
