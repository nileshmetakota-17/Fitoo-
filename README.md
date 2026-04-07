# Fitoo!!

**Fitoo!!** is an Android fitness app (`com.example.fitoo`) that combines **meal logging**, **workout tracking**, optional **step-based activity** on the Home screen, and an in-app AI coach (**Sensei**). Structured data lives in a **Room** SQLite database on the device; there is no Fitoo-owned cloud sync in the app code.

| | |
|---|---|
| **Version** | `1.0` (`versionCode` 1) |
| **Min / target SDK** | 26 / 36 |
| **Language** | Java |
| **UI** | AppCompat, Fragments, Material 3 (Day/Night) |

---

## How the app is structured

- **One activity** — `MainActivity` hosts a **bottom navigation** bar and a single container.
- **Five fragments** — `HomeFragment`, `MealsFragment`, `WorkoutsFragment`, `SenseiFragment`, `ProfileFragment` are **all added once** at startup and **shown/hidden** when you change tabs. That keeps each tab’s state (scroll, inputs) when you switch away and back.
- **Background work** — database and network run on **`AppExecutors`** (shared IO pool + main thread posts), not on the UI thread.

---

## Features (by tab)

### Home

- Today’s **calories** vs **target** (from profile).
- **Macros:** protein (vs optional target), carbs, fats, **fiber**.
- **Rolling calorie totals** for week / month / three months (from meal timestamps).
- **Today’s workout** progress from `workout_log` for the current day; optional filter to focus on “today only”; you can pick how today’s plan is seeded.
- **Activity** — with **Activity Recognition** permission (Android 10+), uses the hardware **step counter** sensor; shows steps, estimated active minutes and move kcal, and **activity rings** vs daily goals.

### Meals

- Log foods with quantity in **count** or **grams**.
- **Local macro estimates** (`NutritionLookup`) for many common foods, including **fiber**.
- **Online lookup** in **grams** mode (`OnlineNutritionLookup`) via **Open Food Facts**, with an in-memory **LRU cache** (not persisted after the app process ends).
- Override macros manually anytime.
- Mark items **eaten**, edit, delete.
- **Diet plan** — template rows stored in Room; **apply to today** inserts matching **meal** rows for the current day.

### Workouts

- **Today’s log** — exercises for the current day with sets/reps and **completed** state.
- **Built-in catalog** plus **custom** exercises; **hide** built-in items (stored in preferences); **restore** defaults.
- **Muscle groups / categories** with management (ordering and visibility in `fitoo_workout_prefs`).
- **Saved routines** — `WorkoutRoutine` + `RoutineExercise` (Room, with foreign key cascade).

### Sensei

- AI **chat** using an **OpenAI API key** (stored in preferences) or a **no-key** HTTP mode.
- **Multiple chat sessions** / history stored in **`fitoo_prefs`**.
- **Action buttons** (when the reply suggests diet/workout changes): apply template **diet plan**, **add plan to today’s meals**, apply template **today’s workout**, and **Undo** (restores snapshots of diet plan, today’s log, or removes last inserted meals).

### Profile

- **Goals** and demographics in **`user_profile`** (Room).
- **Profile photo** saved as an **internal file**; path/URI on the profile row.
- **Delete all data** — wipes app-stored data per implementation in `ProfileFragment`.

---

## How data is stored

### Room (`fitoo.db`)

| Table | Contents |
|-------|----------|
| `meals` | Logged meals: name, category, quantity + unit (`count` / `g`), calories, protein, carbs, fats, fiber, timestamp, eaten |
| `diet_plan` | Diet plan template items (same macro fields + sort order) |
| `workout_log` | Daily entries: `dateDay`, muscle group, exercise, sets, reps, completed |
| `workout_routines` | Named routines |
| `routine_exercises` | Exercises per routine (FK → `workout_routines`, CASCADE delete) |
| `user_profile` | Single-row profile (`id = 1`): name, goals, targets, etc. |

- **Schema version:** `8` (`FitooDatabase`).
- **Migrations:** `fallbackToDestructiveMigration()` — upgrading the app **may reset** the database if the schema version changes (no custom migrations yet).

### SharedPreferences

| File | Typical keys |
|------|----------------|
| `fitoo_prefs` | `openai_api_key`, `openai_model`, `no_key_model`, `sensei_chat_history` |
| `fitoo_workout_prefs` | Muscle group list, `hidden_default_exercises` |

### Other

- **Avatar file** — internal storage (e.g. `profile_avatar.png`).
- **Open Food Facts** — network only; **cache is in-memory** inside the lookup class.

---

## AI configuration (Sensei)

| Mode | Configuration |
|------|----------------|
| **OpenAI** | `fitoo_prefs`: `openai_api_key`; optional `openai_model` (default `gpt-4.1-mini`) |
| **No-key** | If no API key, uses external text API; `no_key_model` (default `openai-fast`) |

---

## Requirements and run

- **Android Studio** (recommended), **Android SDK** (compileSdk 36).
- **JDK 17** for running Gradle / Android Gradle Plugin.
- Run the **`app`** configuration on a device or emulator (**API 26+**).

## Build an APK

```bash
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
(Release builds need signing configured in Android Studio.)

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/FITOO_APPLICATION_DOCUMENTATION.md](docs/FITOO_APPLICATION_DOCUMENTATION.md) | Full write-up: navigation, every tab, **Room tables and fields**, SharedPreferences, files, Sensei actions, permissions |
| `Fitoo_doc.docx` (project root) | Word export of the same documentation (regenerate after editing the markdown: `python3 scripts/md_to_docx.py`) |

---

## Permissions (summary)

- **Internet** — Open Food Facts, AI HTTP clients.
- **Activity Recognition** (Android 10+) — step-based activity on Home when required by the OS.

---

## License

Add your license here if you distribute the project.
