# Vox

Vox is an Android personal finance app with voice transaction capture, a spending dashboard, budgets, recurring payments, group expenses, and receipt scanning. Profile and finance data are stored on the device. A small NestJS API forwards voice recordings to Gnani for transcription and generates spoken answers.

## Project layout

- `vox-android/` — Kotlin and Jetpack Compose Android app, including the home screen widget.
- `vox-backend/` — NestJS voice API. See its [README](vox-backend/README.md) for endpoints and deployment.
- `docs/` — product requirements and the challenge checklist.

## Run the Android app

Use Android Studio with JDK 21 and Android SDK 36.1. The app supports Android 8.0 (API 26) and later. Open `vox-android/`, let Gradle sync, select a device or emulator, and run the `app` configuration. Complete the device profile on first launch.

To build from a terminal with your JDK and Android SDK configured:

```sh
cd vox-android
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The debug APK is written to `vox-android/app/build/outputs/apk/debug/app-debug.apk`. The default voice API URL is configured in `vox-android/app/build.gradle.kts`. To use a local API from the Android emulator:

```sh
VOX_API_BASE_URL=http://10.0.2.2:3000/api/v1 ./gradlew :app:installDebug
```

For a physical phone, use a reachable HTTPS API URL or your development machine's LAN address with a debug build. The API URL is baked into the APK; rebuild after changing it. Keep Gnani credentials on the server.

## Run the voice API locally

Use Node.js 22 or later. Generate a Gnani key with speech-to-text and text-to-speech access, then:

```sh
cd vox-backend
cp .env.example .env
# Set GNANI_API_KEY in .env before starting the server.
npm ci
npm run build
node --env-file=.env dist/main.js
```

The health endpoint is `http://localhost:3000/api/v1/health`. The active demo module needs no database; the Prisma and worker files are retained from an earlier API implementation. The demo voice endpoints do not require authentication; add authentication and abuse controls before a public rollout.

## Record from the home screen

1. Install Vox and complete the device profile once.
2. In Vox's account/settings panel, choose **Add home screen widget**, then confirm placement in the launcher. Alternatively, long-press an empty area of your home screen, choose **Widgets → Vox → Vox · Voice transaction**, and drag it into place. If the launcher does not support the in-app add button, use its widget picker.
3. Tap the widget. A voice panel appears over your home screen and requests microphone permission if needed. Once allowed, recording starts automatically for five seconds.
4. Say a transaction such as **“Spent 400 rupees on groceries”** or **“Received 1200 rupees salary.”** Wait for transcription, check the amount, type and category, then tap **Confirm**. The panel closes and displays **Transaction saved**. The transaction is available in Vox's dashboard and activity list.

If recognition is wrong, choose **Try again**, or close the panel without saving. Cancel recording to return to the microphone and language controls; select **हिन्दी / Hinglish** there before recording again. Parsing currently uses numeric amounts, saves in INR, and defaults to UPI. Review the result before confirming.

The widget uses a dedicated recording activity, so you do not need to navigate into the main app. Recording stops when the panel leaves the foreground. Nothing is saved until you confirm; repeated confirmation taps are ignored while saving. A failed save keeps the recognized transaction for retry. Budget commands and spending questions belong in the main app's voice assistant.

Voice transcription requires internet access and a working voice API. Microphone denial shows instructions for granting access in Android Settings. If the device profile is missing or has been signed out, the widget prompts you to set it up in Vox first. The widget displays no balances or transaction history.

## Checks

```sh
cd vox-android
./gradlew :app:testDebugUnitTest :app:assembleDebug
# With an emulator or device connected:
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.vox.widget.VoiceCaptureTest
```

The widget flow tests cover confirmation, duplicate taps, save retries, recording cancellation, and rejecting commands that would change a budget. They use fake voice and transaction repositories and do not call Gnani.

For a device smoke test, add the widget, try microphone denial and approval, record and confirm an expense, verify it in Activity, and close a second recording without saving. Also try leaving the recording panel with Home and reopening the widget while the main app already has a task in Recents.
