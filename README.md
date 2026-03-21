# Fitoo!!

**Fitoo!!** is an Android fitness app for logging **meals**, tracking **workouts**, and chatting with an in-app AI coach (**Sensei**). Data stays on your device.

| | |
|---|---|
| **Version** | `1.0` (`versionCode` 1) |
| **Min Android** | 8.0 (API 26) |
| **Package** | `com.example.fitoo` |

---

## Quick start

1. Clone or open this folder in **Android Studio**.
2. Use **JDK 17** (required by the Android Gradle Plugin).
3. **Run** the `app` configuration on a device or emulator, **or** build an APK (see below).

---

## Build & install the APK

There is no pre-built APK in the repo—you generate it on your machine.

### Option A — Android Studio (easiest)

1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. When finished, click **locate** in the notification, or open:
   - **Debug:** `app/build/outputs/apk/debug/app-debug.apk`
   - **Release:** `app/build/outputs/apk/release/app-release.apk` (unsigned unless you configure signing)

### Option B — Command line

From the project root:

```bash
cd /path/to/Fitoo
./gradlew :app:assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

For a release build (configure signing in Android Studio first):

```bash
./gradlew :app:assembleRelease
```

**Requirements:** JDK **17**, Android SDK (via Android Studio or `ANDROID_HOME`).

### Install on a phone

- Copy `app-debug.apk` to the device and open it (install unknown sources if prompted), **or**
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`

---

## Features (summary)

| Area | What you get |
|------|----------------|
| **Home** | Daily calories, macros (protein / carbs / fats / **fiber**), rolling calorie totals, today’s workout progress |
| **Meals** | Add food by **count** or **grams;** local estimates + **online lookup** for unknown foods (grams); diet plan; apply plan to today |
| **Workouts** | Today’s log, built-in + custom exercises, **hide** built-in items, categories, history |
| **Sensei** | AI chat (OpenAI key **or** no-key mode); actions to apply diet/workout changes + **Undo** |
| **Profile** | Goals, photo, wipe all data |

Details: [`docs/FEATURES.md`](docs/FEATURES.md).

---

## Tech stack

- **UI:** AppCompat, Fragments, Material 3
- **Language:** Java
- **Storage:** Room (SQLite)
- **Network:** `HttpURLConnection` (AI + Open Food Facts lookup)
- **Threading:** `AppExecutors` for smoother UI

---

## Sensei (optional)

| Mode | How |
|------|-----|
| **OpenAI** | Set prefs in `fitoo_prefs`: `openai_api_key`, optional `openai_model` (default `gpt-4.1-mini`) |
| **No-key** | If no key, uses external text API; model `no_key_model` (default `openai-fast`) |

See [`docs/SETUP.md`](docs/SETUP.md).

---

## Data on device

| Data | Location |
|------|-----------|
| Database | `fitoo.db` (Room) |
| AI + Sensei chats | `fitoo_prefs` |
| Workout prefs | `fitoo_workout_prefs` |
| Profile image | `profile_avatar.png` (app storage) |

---

## Project layout

```
app/src/main/java/com/example/fitoo/   # App code
app/src/main/res/                      # Layouts, drawables, strings
docs/                                  # Extra documentation
```

---

## Documentation

| File | Description |
|------|-------------|
| [`docs/SETUP.md`](docs/SETUP.md) | Build, SDK, AI prefs |
| [`docs/FEATURES.md`](docs/FEATURES.md) | Feature walkthrough |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Structure & layers |
| [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) | Room entities |
| [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) | Gradle, Java 17, Sensei |

---

## Troubleshooting (short)

- **Gradle wants Java 17:** install JDK 17 and point Android Studio / `JAVA_HOME` to it.
- **No APK file:** run **Build APK** or `./gradlew :app:assembleDebug` and use the path under `app/build/outputs/apk/`.

More: [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md).

---

## License

Add your license here if you want this project to be open source.
