# Setup & Build

## Prerequisites

- Android Studio (recommended)
- JDK **17** (required by Android Gradle Plugin)
- Android SDK installed (project uses `compileSdk = 36`, `minSdk = 26`)

## Open in Android Studio

1. Open Android Studio → **Open** → select the project root (`Fitoo/`).
2. Wait for Gradle sync to finish.
3. Run the `app` configuration on an emulator/device (API 26+).

## Build from command line (optional)

From the project root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Outputs:

- Debug APK: `app/build/outputs/apk/debug/`
- Release APK: `app/build/outputs/apk/release/`

## AI / Sensei configuration

Sensei supports two modes:

### With OpenAI API key

Stored in `SharedPreferences`:

- Prefs file: `fitoo_prefs`
- `openai_api_key`: your key (non-empty enables OpenAI)
- `openai_model`: model name (default `gpt-4.1-mini`)

Implementation reference:

- `app/src/main/java/com/example/fitoo/AiPreferences.java`
- `app/src/main/java/com/example/fitoo/SenseiFragment.java`
- `app/src/main/java/com/example/fitoo/OpenAiClient.java`

### No-key mode

If `openai_api_key` is empty, Sensei uses a no-key backend:

- `no_key_model` (default `openai-fast`)
- Model list is fetched dynamically from the provider.

Implementation reference:

- `app/src/main/java/com/example/fitoo/FreeAiClient.java`

## Local data and reset

Local storage used by the app:

- Room database: `fitoo.db`
- SharedPreferences: `fitoo_prefs`
- SharedPreferences: `fitoo_workout_prefs` (workout categories + hidden default exercises)
- Profile avatar file: `profile_avatar.png` (internal storage)

The Profile screen offers “Delete all data” to reset the app state.

