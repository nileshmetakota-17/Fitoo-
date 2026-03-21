# Data model (Room + local files)

## Room database

- Database: `FitooDatabase`
- File: `fitoo.db`
- Migration strategy: `fallbackToDestructiveMigration()` (schema changes wipe data)

### Entities

The database declares these entities:

- `MealEntry`
  - A food item logged for a specific timestamp
  - `eaten` controls whether it counts toward totals
  - includes macros: calories/protein/carbs/fats/**fiber**
- `DietPlanItem`
  - A reusable template food item for the “diet plan”
  - includes macros: calories/protein/carbs/fats/**fiber**
- `WorkoutLogEntry`
  - A daily exercise row (`dateDay` at start-of-day)
  - Tracks `muscleGroup`, `exerciseName`, sets/reps, `completed`
- `WorkoutRoutine`
  - A named routine
- `RoutineExercise`
  - Exercises inside a routine with ordering
- `UserProfile`
  - Profile identity and targets (calories/protein), plus optional `photoUri`

### DAOs (entry points)

The database exposes:

- `MealDao`
- `DietPlanDao`
- `WorkoutLogDao`
- `WorkoutRoutineDao`
- `RoutineExerciseDao`
- `UserProfileDao`

These are called directly from Fragments on background threads.

## SharedPreferences

### `fitoo_prefs`

Used for AI settings and Sensei chat sessions:

- `openai_api_key`
- `openai_model` (default: `gpt-4.1-mini`)
- `no_key_model` (default: `openai-fast`)
- Sensei chat history (JSON serialized sessions)

Workouts also store category names in a separate prefs file:

- `fitoo_workout_prefs` → `muscle_groups`
- `fitoo_workout_prefs` → `hidden_default_exercises` (hidden built-in exercises)

## Files

- Profile avatar: internal storage file `profile_avatar.png`

