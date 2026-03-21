# Features (Walkthrough)

## Navigation

`MainActivity` hosts a bottom navigation with 5 tabs:

- **Home**: dashboard + “today plan” progress
- **Meals**: daily meal list + diet plan
- **Workouts**: today’s exercises + history
- **Sensei**: AI fitness coach chat
- **Profile**: user profile, targets, and data reset

## Home (Dashboard)

`HomeFragment` shows:

- **Today summary**: calories + macros (protein/carbs/fats/**fiber**) counted from foods marked as “eaten”
- **Targets**: reads from `UserProfile` if configured
- **Rolling totals**: last 7 / 30 / 90 days calories (eaten only)
- **Today’s workout plan**: checklist-style progress based on today’s workout log

Also includes:

- **Restart reports**: clears meal log + workout log
- **Select categories/exercises for today**: generates today’s plan from a built-in catalog and stores it in the workout log

## Meals

`MealsFragment` provides:

- Add/edit food items for today with:
  - quantity in **count** or **grams**
  - category (Breakfast/Lunch/Snacks/Dinner)
  - optional manual macros override
- Auto macro estimates (incl. **fiber**) for common foods via `NutritionLookup`
- For unknown foods (grams mode): **online nutrition lookup** (cached for faster repeats)
- Mark items as eaten (affects totals on Home + Meals)
- Sort by time or calories

Diet plan:

- Save template meals (`DietPlanItem`)
- Apply template items to “today” as uneaten meals

## Workouts

`WorkoutsFragment` provides:

- Daily workout list grouped by muscle category
- Completion tracking per exercise (checkbox)
- Add/edit/delete custom exercises
- Delete/hide built-in exercises (restore defaults from Workouts screen)
- Manage workout categories (persisted in `SharedPreferences`)
- Recent-day “history” summary (days + exercise count)

## Routine session (standalone screen)

`RoutineSessionActivity` is a standalone checklist screen that can load a saved routine by ID or work in “empty workout” mode.

## Sensei (AI coach)

`SenseiFragment` is an in-app chat UI with:

- Conversation memory (trimmed to keep it small)
- Chat sessions history (multiple sessions saved locally)
- Two backends:
  - **OpenAI** if an API key exists
  - **No-key** provider if not
- Model picker for the no-key provider (loaded dynamically)
- Action buttons to apply:
  - diet plan changes
  - adding diet plan to today’s meals
  - workout changes
  - Undo for the last applied changes

## Profile

`ProfileFragment` provides:

- Profile info: name, age, gender, height, weight, goal
- Targets: calories + protein (used by Meals/Home displays)
- Profile photo pick + crop (saved internally)
- “Delete all app data” reset (clears DB + prefs + files)

