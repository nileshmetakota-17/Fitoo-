# Fitoo!! — Application documentation

This document describes how **Fitoo!!** works end to end: navigation, features, and where data lives (Room, preferences, files).

---

## 1. Overview

**Fitoo!!** (`com.example.fitoo`) is a native **Android** app written in **Java**. It combines:

- Daily **nutrition logging** (meals, macros, diet plan templates)
- **Workout** planning and logging (built-in catalog, custom exercises, categories)
- Optional **step-based activity** on the Home screen (hardware step counter + Activity Recognition permission on Android 10+)
- An in-app AI coach (**Sensei**) for chat, with optional actions that update the diet plan or today’s workout (with **Undo**)

All primary structured data is stored **on the device** in a **Room** database (`fitoo.db`). AI keys, Sensei chat history, and some workout UI preferences use **SharedPreferences**. The profile photo is a file in app-internal storage.

The UI follows **Material 3** (including Day/Night). A **single activity** (`MainActivity`) hosts **five fragments** behind a **bottom navigation** bar.

---

## 2. Architecture and navigation

### 2.1 Single activity, multiple fragments

- **`MainActivity`** sets the content view, plays a looping background video, and hosts a `BottomNavigationView`.
- On first launch, **all five fragments are added** to the fragment manager at once (`add`), each with a tag (`home`, `meals`, `workouts`, `sensei`, `settings`). Only **Home** is shown; the others are **hidden**.
- Tab changes use **show/hide**, not replace. This **preserves each tab’s state** (scroll position, form state) when switching away and back.

Bottom navigation destinations:

| Tab (concept) | Fragment | Tag |
|---------------|----------|-----|
| Home | `HomeFragment` | `home` |
| Meals | `MealsFragment` | `meals` |
| Workouts | `WorkoutsFragment` | `workouts` |
| Sensei | `SenseiFragment` | `sensei` |
| Profile / settings | `ProfileFragment` | `settings` |

### 2.2 Background work

**`AppExecutors`** provides shared **main** and **IO** executors so database and network work do not block the UI thread.

---

## 3. Data storage

### 3.1 Room database (`fitoo.db`)

- **File name:** `fitoo.db` (created by Room in the app’s private storage).
- **Version:** `8` (see `FitooDatabase`).
- **Migrations:** the app uses **`fallbackToDestructiveMigration()`**. Schema upgrades **drop and recreate** the database. Users should treat this as “data may reset on app update” unless migrations are added later.

**Entities and tables:**

| Room entity | SQLite table | Role |
|-------------|--------------|------|
| `MealEntry` | `meals` | Each logged food item: name, category, quantity + unit (`count` or `g`), calories, protein, carbs, fats, fiber, timestamp, `eaten` flag |
| `DietPlanItem` | `diet_plan` | Rows in “My Diet Plan” template; can be applied to “today” as real `MealEntry` rows |
| `WorkoutLogEntry` | `workout_log` | Per-day log lines: `dateDay`, muscle group, exercise name, sets, reps, `completed` |
| `WorkoutRoutine` | `workout_routines` | Named saved routines |
| `RoutineExercise` | `routine_exercises` | Exercises belonging to a routine; **foreign key** to `workout_routines.id` with **CASCADE** delete |
| `UserProfile` | `user_profile` | Single logical profile row (`id = 1`): name, demographics, goals, targets (e.g. calories, protein), `photoUri` string |

**DAOs** (via `FitooDatabase`): `MealDao`, `DietPlanDao`, `WorkoutLogDao`, `WorkoutRoutineDao`, `RoutineExerciseDao`, `UserProfileDao`.

### 3.2 SharedPreferences

**`fitoo_prefs`** (`AiPreferences`):

- `openai_api_key` — OpenAI API key for Sensei (optional)
- `openai_model` — model name (default `gpt-4.1-mini`)
- `no_key_model` — model when using no-key fallback (default `openai-fast`)
- `sensei_chat_history` — serialized Sensei conversation / sessions

**`fitoo_workout_prefs`** (`WorkoutsFragment`):

- Muscle group ordering / custom list (key such as `muscle_groups`)
- `hidden_default_exercises` — which built-in seed exercises are hidden from the catalog

### 3.3 Files

- **Profile avatar:** saved under internal storage (e.g. `profile_avatar.png` or path referenced from profile), not inside Room.

### 3.4 Network (not “stored” as a database)

- **Open Food Facts** — used for **online nutrition lookup** in grams mode; results are **cached in memory** (LRU, per-query key) inside `OnlineNutritionLookup`, not persisted across process death.

---

## 4. Features by screen

### 4.1 Home

- **Calories** consumed today vs **target** from `UserProfile`.
- **Macros:** protein (with optional comparison to target), carbs, fats, **fiber**.
- **Rolling calorie totals** for week / month / three months (aggregated from `meals` by timestamps).
- **Today’s workout plan:** progress toward completing today’s `workout_log` entries; can show only today’s plan or a broader view (UI switch); optional dialog to pick which routine seeds today’s log.
- **Activity card:** if **Activity Recognition** is granted (Android 10+), reads **`TYPE_STEP_COUNTER`**; derives approximate **active minutes** and **move kcal** from steps; **`ActivityRingsView`** shows three ring fills vs goals (steps, minutes, move kcal). Without permission, shows a prompt to allow activity recognition.

### 4.2 Meals

- Add meals with quantity in **count** or **grams**.
- **Local estimates** via `NutritionLookup` for common foods; **fiber** included.
- **Online lookup** (`OnlineNutritionLookup`) for unknown foods when using **grams** — queries Open Food Facts, scores matches, scales per-100g values to the entered grams.
- Manual override of macros always possible.
- Mark meals **eaten**, edit, delete.
- **Diet plan** (`diet_plan` table): maintain template items; **apply plan to today** inserts corresponding rows into `meals`.

### 4.3 Workouts

- **Today’s log** in `workout_log` for the current calendar day (`dateDay`).
- **Built-in exercises** (seed list) plus **custom** exercises; **hide** built-in entries (stored in `fitoo_workout_prefs`); **restore** hidden defaults.
- **Categories** (muscle groups) with management and reassignment when deleting categories.
- **Workout routines** in `workout_routines` / `routine_exercises` for saved templates.
- Checking off predefined exercises can create or align with today’s log entries (per app logic in `WorkoutsFragment`).

### 4.4 Sensei

- Chat UI with **OpenAI** (if key set) or **no-key** HTTP client.
- **Chat history** in `fitoo_prefs`.
- When the assistant’s text matches simple **keyword heuristics** (e.g. phrases like “diet plan”, “modify workout”), **action buttons** may appear, for example:
  - **Apply Diet Plan Change** — replaces `diet_plan` with a deterministic template (snapshot kept for Undo).
  - **Add plan to today’s meals** — copies current `diet_plan` rows into `meals` for today (tracks inserted IDs for Undo).
  - **Apply Workout Change** — replaces **today’s** `workout_log` with a small template (snapshot of previous day entries for Undo).
  - **Undo** — restores diet plan snapshot, today’s workout snapshot, or removes last inserted meals, depending on what ran last.

### 4.5 Profile

- Edit **user profile** (stored in `user_profile`).
- Goals: e.g. target calories, protein.
- **Photo** pick/crop → saved to internal file; URI/path referenced from profile.
- **Delete all data** — clears app data paths as implemented (Room + prefs + files per `ProfileFragment` logic).

---

## 5. Permissions and sensors

- **`ACTIVITY_RECOGNITION`** (Android 10+): required for step counter–based activity on Home when the OS mandates it.
- **Internet:** used for Open Food Facts and AI HTTP clients (not for syncing a Fitoo cloud backend — there is no first-party cloud storage described in code).

---

## 6. Build and version

- **Min SDK:** 26 · **Target / compile:** 36 (see `app/build.gradle.kts`).
- **Version:** `versionName` `1.0`, `versionCode` `1`.
- **JDK:** Android Gradle Plugin requires **JDK 17** to run Gradle; Java language level in module may be 11 for source/target compatibility.

**Debug APK** (after successful build):

`app/build/outputs/apk/debug/app-debug.apk`

---

## 7. Related project files

- `README.md` — short project overview and quick start
- `app/src/main/java/com/example/fitoo/FitooDatabase.java` — Room entry point
- Entity classes: `MealEntry`, `DietPlanItem`, `WorkoutLogEntry`, `WorkoutRoutine`, `RoutineExercise`, `UserProfile`

---

*This documentation matches the app’s behavior as implemented in source. If the codebase changes, update this file and the README accordingly.*
