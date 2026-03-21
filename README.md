# Fitoo!! (Android)

Fitoo!! is an Android fitness tracker that combines **meal logging**, **workout tracking**, and an in-app AI coach (**Sensei**) to help you stay consistent.

## Features

- **Home dashboard**
  - Today’s consumed calories + macros (protein/carbs/fats/fiber)
  - Rolling totals for week / month / 3 months
  - “Today’s workout plan” progress
- **Meals**
  - Add foods with quantity in **count** or **grams**
  - Auto macro estimates for common foods + **fiber**
  - **Online nutrition lookup** for unknown foods (grams mode) + caching for faster results
  - Manual macro override (always supported)
  - Mark foods as eaten, edit, delete
  - Simple **diet plan** you can apply to “today”
- **Workouts**
  - Daily workout log with completion tracking
  - Built-in exercise catalog + add custom exercises
  - **Hide/remove built-in exercises** (restore defaults anytime)
  - Manage workout categories (reassign on delete)
  - Recent day history
- **Profile**
  - Save profile + goals (target calories / protein)
  - Pick and crop a profile photo (saved to internal storage)
  - “Delete all data” reset
- **Sensei (AI coach)**
  - Chat about training / nutrition inside the app
  - Works **with an OpenAI API key** or in **no-key mode** (uses a third-party text endpoint)
  - Multiple saved chat sessions (history)
  - Action buttons to apply changes (diet/workout) + **Undo**

## Tech stack

- **Android**: AppCompat + Fragments + Material UI
- **Language**: Java
- **Persistence**: Room (SQLite)
- **Networking**: `HttpURLConnection` (JSON over HTTP)
- **Performance**: shared background executor (`AppExecutors`) to keep UI responsive

## Requirements

- Android Studio (recommended)
- JDK **17** (Android Gradle Plugin requires Java 17 to run)
- Android SDK installed (project targets `compileSdk = 36`, `minSdk = 26`)

## Run the app

1. Open the project root folder in Android Studio.
2. Let Gradle sync finish.
3. Select the `app` run configuration.
4. Run on an emulator or a device (Android 8.0 / API 26+).

## AI configuration (optional)

Sensei supports two modes:

### 1) With OpenAI API key (recommended for reliability)

The app stores the API key in Android `SharedPreferences` under `fitoo_prefs`.

- **Prefs name**: `fitoo_prefs`
- **Key**: `openai_api_key`
- **Model key**: `openai_model` (default: `gpt-4.1-mini`)

If you don’t set a key, Sensei automatically falls back to “no-key” mode.

### 2) No-key mode (fallback)

Uses a public text endpoint and a selectable “no-key model”.

- **Pref key**: `no_key_model` (default: `openai-fast`)

Note: Availability and model list depend on the external provider.

## Data storage

All app data is stored locally:

- **Room database**: `fitoo.db`
- **Preferences**: `fitoo_prefs` (AI settings + Sensei chat sessions)
- **Preferences**: `fitoo_workout_prefs` (workout categories + hidden default exercises)
- **Profile avatar**: internal file `profile_avatar.png`

## Project structure

- `app/src/main/java/com/example/fitoo/`
  - `MainActivity` (bottom navigation host)
  - `HomeFragment`, `MealsFragment`, `WorkoutsFragment`, `SenseiFragment`, `ProfileFragment`
  - Room: `FitooDatabase` + entities/DAOs
  - AI: `OpenAiClient`, `FreeAiClient`, `AiPreferences`
- `app/src/main/res/` (layouts, drawables, strings, etc.)

## Documentation

- `docs/SETUP.md` — setup, build, and configuration
- `docs/FEATURES.md` — user-facing feature walkthrough
- `docs/ARCHITECTURE.md` — high-level architecture and module map
- `docs/DATA_MODEL.md` — Room entities and stored data
- `docs/TROUBLESHOOTING.md` — common issues and fixes

## Versioning / “v1 APK”

This snapshot is **Version 1**:

- `versionCode = 1`
- `versionName = "1.0"`

To generate APKs (requires Java 17):

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

APKs will be created under `app/build/outputs/apk/`.

