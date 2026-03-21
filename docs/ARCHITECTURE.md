# Architecture

## High-level design

Fitoo is a single-module Android app (`:app`) built with:

- `MainActivity` hosting five Fragments (bottom navigation)
- Room database (`FitooDatabase`) for persistent user data
- SharedPreferences (`fitoo_prefs`) for AI settings + Sensei chat sessions
- Simple HTTP clients for AI requests

## UI flow

- `MainActivity`
  - loads and keeps these fragments attached (then shows/hides them):
    - `HomeFragment`
    - `MealsFragment`
    - `WorkoutsFragment`
    - `SenseiFragment`
    - `ProfileFragment`

This approach keeps fragment state alive while switching tabs (no re-create on each tab change).

## Data layer

Room database:

- `FitooDatabase` (name: `fitoo.db`, destructive migrations enabled)
- Entities include:
  - `MealEntry`
  - `DietPlanItem`
  - `WorkoutLogEntry`
  - `WorkoutRoutine`, `RoutineExercise` (saved routines)
  - `UserProfile`

DAOs are used directly from Fragments (background threads + UI updates on main thread).

## Sensei AI layer

Two backends:

- `OpenAiClient`
  - Endpoint: `https://api.openai.com/v1/chat/completions`
  - Used only when `openai_api_key` exists in prefs.
- `FreeAiClient`
  - Uses a “no-key” text provider for chat and model list
  - Chosen when no API key is configured

Preferences are defined in `AiPreferences`.

Sensei also provides **action buttons** that can apply changes directly to local data:

- Updates diet plan (Room: `diet_plan`)
- Adds diet plan entries to today (Room: `meals`)
- Updates today’s workout (Room: `workout_log`)
- Undo by restoring snapshots

## Concurrency model

The app primarily uses:

- `AppExecutors` (shared background thread pool) for DB + network tasks
- `ExecutorService` for Sensei chat requests (single-thread executor)

UI updates are posted back via `Activity.runOnUiThread(...)`.

## Background / reminders

- `ReminderReceiver` exists as a BroadcastReceiver (currently shows a toast on receive)
- Manifest includes INTERNET permission.

