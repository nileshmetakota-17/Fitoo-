package com.example.fitoo;

import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final SeedExercise[] DEFAULT_EXERCISES = {
            new SeedExercise("Chest", "Barbell Bench Press", 4, 8),
            new SeedExercise("Chest", "Incline Dumbbell Press", 4, 10),
            new SeedExercise("Chest", "Dumbbell Chest Fly", 3, 12),
            new SeedExercise("Chest", "Push-ups", 3, 15),
            new SeedExercise("Chest", "Cable Crossover", 3, 12),
            new SeedExercise("Back", "Deadlifts", 4, 6),
            new SeedExercise("Back", "Lat Pulldown", 4, 10),
            new SeedExercise("Back", "Bent Over Barbell Row", 4, 10),
            new SeedExercise("Back", "Seated Cable Row", 3, 12),
            new SeedExercise("Back", "Face Pull", 3, 12),
            new SeedExercise("Biceps", "Barbell Curl", 4, 10),
            new SeedExercise("Biceps", "Hammer Curl", 3, 12),
            new SeedExercise("Biceps", "Preacher Curl", 3, 10),
            new SeedExercise("Triceps", "Triceps Pushdown", 4, 12),
            new SeedExercise("Triceps", "Skull Crushers", 3, 10),
            new SeedExercise("Triceps", "Bench Dips", 3, 12),
            new SeedExercise("Shoulders", "Barbell Overhead Press", 4, 8),
            new SeedExercise("Shoulders", "Dumbbell Shoulder Press", 3, 10),
            new SeedExercise("Shoulders", "Lateral Raises", 4, 12),
            new SeedExercise("Shoulders", "Rear Delt Fly", 3, 12),
            new SeedExercise("Shoulders", "Shrugs", 3, 15),
            new SeedExercise("Legs", "Barbell Squats", 4, 8),
            new SeedExercise("Legs", "Leg Press", 4, 10),
            new SeedExercise("Legs", "Leg Extension", 3, 12),
            new SeedExercise("Legs", "Hamstring Curl", 3, 12),
            new SeedExercise("Legs", "Standing Calf Raise", 4, 15),
            new SeedExercise("Core", "Hanging Leg Raises", 4, 12),
            new SeedExercise("Core", "Cable Crunch", 3, 15),
            new SeedExercise("Core", "Russian Twist", 3, 20),
            new SeedExercise("Core", "Plank", 3, 1),
            new SeedExercise("Core", "Cardio (Treadmill / Cycling)", 15, 20)
    };

    private TextView txtCaloriesValue;
    private TextView txtCaloriesTarget;
    private TextView txtProtein;
    private TextView txtCarbs;
    private TextView txtFats;
    private TextView txtFiber;
    private TextView txtMealsCount;
    private TextView txtWeekCalories;
    private TextView txtMonthCalories;
    private TextView txtQuarterCalories;
    private TextView txtHomeWorkoutProgressLabel;
    private ProgressBar progressCalories;
    private ProgressBar homeWorkoutProgressBar;
    private Button btnRestartReports;
    private LinearLayout containerHomeWorkoutGroups;
    private Switch switchShowTodayWorkoutOnly;

    private FitooDatabase db;
    private final Map<String, Boolean> workoutSectionExpanded = new LinkedHashMap<>();
    private boolean showTodayWorkoutOnly = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        db = FitooDatabase.get(requireContext());

        txtCaloriesValue = v.findViewById(R.id.txtCaloriesValue);
        txtCaloriesTarget = v.findViewById(R.id.txtCaloriesTarget);
        txtProtein = v.findViewById(R.id.txtProtein);
        txtCarbs = v.findViewById(R.id.txtCarbs);
        txtFats = v.findViewById(R.id.txtFats);
        txtFiber = v.findViewById(R.id.txtFiber);
        txtMealsCount = v.findViewById(R.id.txtMealsCount);
        txtWeekCalories = v.findViewById(R.id.txtWeekCalories);
        txtMonthCalories = v.findViewById(R.id.txtMonthCalories);
        txtQuarterCalories = v.findViewById(R.id.txtQuarterCalories);
        txtHomeWorkoutProgressLabel = v.findViewById(R.id.txtHomeWorkoutProgressLabel);
        progressCalories = v.findViewById(R.id.progressCalories);
        homeWorkoutProgressBar = v.findViewById(R.id.homeWorkoutProgressBar);
        containerHomeWorkoutGroups = v.findViewById(R.id.containerHomeWorkoutGroups);
        switchShowTodayWorkoutOnly = v.findViewById(R.id.switchShowTodayWorkoutOnly);
        btnRestartReports = v.findViewById(R.id.btnRestartReports);
        Button btnSelectHomeWorkoutPlan = v.findViewById(R.id.btnSelectHomeWorkoutPlan);
        btnRestartReports.setOnClickListener(view -> showResetDialog());
        btnSelectHomeWorkoutPlan.setOnClickListener(view -> showSelectTodayPlanDialog());
        switchShowTodayWorkoutOnly.setChecked(showTodayWorkoutOnly);
        switchShowTodayWorkoutOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showTodayWorkoutOnly = isChecked;
            loadTodayWorkoutPlan();
        });

        loadDashboard();
        loadTodayWorkoutPlan();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
        loadTodayWorkoutPlan();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadDashboard();
            loadTodayWorkoutPlan();
        }
    }

    public void refreshData() {
        loadDashboard();
        loadTodayWorkoutPlan();
    }

    private void showResetDialog() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Restart reports?")
                .setMessage("This will clear meal and workout logs and reset dashboard values to zero.")
                .setPositiveButton("Restart", (d, w) -> resetAllStats())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetAllStats() {
        AppExecutors.get().io().execute(() -> {
            db.mealDao().deleteAll();
            db.workoutLogDao().deleteAll();
            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                if (!isAdded()) {
                    return;
                }
                loadDashboard();
                loadTodayWorkoutPlan();
                Toast.makeText(getContext(), "Reports restarted.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadDashboard() {
        AppExecutors.get().io().execute(() -> {
            long now = System.currentTimeMillis();
            long startToday = startOfDay(now);
            long endToday = endOfDay(now);

            List<MealEntry> todayMeals = db.mealDao().getMealsBetween(startToday, endToday);
            float todayCal = 0f;
            float todayProt = 0f;
            float todayCarb = 0f;
            float todayFat = 0f;
            float todayFiber = 0f;
            int eatenMeals = 0;
            for (MealEntry meal : todayMeals) {
                if (meal.eaten) {
                    eatenMeals++;
                    todayCal += meal.calories;
                    todayProt += meal.protein;
                    todayCarb += meal.carbs;
                    todayFat += meal.fats;
                    todayFiber += meal.fiber;
                }
            }

            long weekFrom = startToday - 6L * 24L * 60L * 60L * 1000L;
            long monthFrom = startToday - 29L * 24L * 60L * 60L * 1000L;
            long threeMonthFrom = startToday - 89L * 24L * 60L * 60L * 1000L;

            float weekCal = sumEatenCalories(db.mealDao().getMealsBetween(weekFrom, endToday));
            float monthCal = sumEatenCalories(db.mealDao().getMealsBetween(monthFrom, endToday));
            float threeCal = sumEatenCalories(db.mealDao().getMealsBetween(threeMonthFrom, endToday));
            UserProfile profile = db.userProfileDao().get();
            int targetCalories = 2000;
            if (profile != null && profile.targetCalories > 0) {
                targetCalories = Math.max(1, Math.round(profile.targetCalories));
            }
            float targetProtein = 0f;
            if (profile != null && profile.targetProtein > 0) {
                targetProtein = Math.max(1f, profile.targetProtein);
            }
            final float finalTodayCal = todayCal;
            final float finalTodayProt = todayProt;
            final float finalTodayCarb = todayCarb;
            final float finalTodayFat = todayFat;
            final float finalTodayFiber = todayFiber;
            final int finalEatenMeals = eatenMeals;
            final float finalWeekCal = weekCal;
            final float finalMonthCal = monthCal;
            final float finalThreeCal = threeCal;
            final int finalTargetCalories = targetCalories;
            final float finalTargetProtein = targetProtein;

            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }

            AppExecutors.get().main().execute(() -> {
                if (!isAdded()) {
                    return;
                }
                int cal = Math.round(finalTodayCal);
                txtCaloriesValue.setText(String.valueOf(cal));
                txtCaloriesTarget.setText("of " + finalTargetCalories + " kcal target");

                int progress = Math.min(100, (int) ((finalTodayCal / finalTargetCalories) * 100f));
                progressCalories.setProgress(progress);

                txtProtein.setText(buildMacroLine("Protein", finalTodayProt, "g", finalTargetProtein));
                txtCarbs.setText("Carbs: " + Math.round(finalTodayCarb) + " g");
                txtFats.setText("Fats: " + Math.round(finalTodayFat) + " g");
                txtFiber.setText("Fiber: " + Math.round(finalTodayFiber) + " g");

                txtMealsCount.setText(finalEatenMeals + " meals eaten");

                txtWeekCalories.setText(String.valueOf(Math.round(finalWeekCal)));
                txtMonthCalories.setText(String.valueOf(Math.round(finalMonthCal)));
                txtQuarterCalories.setText(String.valueOf(Math.round(finalThreeCal)));
            });
        });
    }

    private String buildMacroLine(@NonNull String label, float eaten, @NonNull String unit, float target) {
        if (target > 0f) {
            int percent = Math.max(0, Math.min(999, Math.round((eaten / target) * 100f)));
            return label + ": " + Math.round(eaten) + " " + unit + " (" + percent + "% of " + Math.round(target) + " " + unit + ")";
        }
        return label + ": " + Math.round(eaten) + " " + unit;
    }

    private void loadTodayWorkoutPlan() {
        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            List<WorkoutLogEntry> todayEntries = db.workoutLogDao().getByDay(today);
            int completed = 0;
            for (WorkoutLogEntry entry : todayEntries) {
                if (entry.completed) {
                    completed++;
                }
            }

            final int total = todayEntries.size();
            final int finalCompleted = completed;
            final int progress = total > 0 ? (finalCompleted * 100 / total) : 0;
            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                if (!isAdded()) {
                    return;
                }
                if (!showTodayWorkoutOnly) {
                    txtHomeWorkoutProgressLabel.setText("Turn on the toggle to show today's workout.");
                    txtHomeWorkoutProgressLabel.setVisibility(View.VISIBLE);
                    homeWorkoutProgressBar.setVisibility(View.GONE);
                    containerHomeWorkoutGroups.setVisibility(View.GONE);
                    return;
                }

                txtHomeWorkoutProgressLabel.setText("Today's plan: " + finalCompleted + "/" + total);
                txtHomeWorkoutProgressLabel.setVisibility(View.VISIBLE);
                homeWorkoutProgressBar.setVisibility(View.VISIBLE);
                containerHomeWorkoutGroups.setVisibility(View.VISIBLE);
                animateChakraProgress(homeWorkoutProgressBar, progress);
                renderHomeWorkoutGroups(todayEntries);
            });
        });
    }

    private void showSelectTodayPlanDialog() {
        List<String> catalogCategories = getCatalogCategories();
        if (catalogCategories.isEmpty() || getContext() == null) {
            return;
        }

        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            List<WorkoutLogEntry> todayEntries = db.workoutLogDao().getByDay(today);
            List<String> selectedToday = new ArrayList<>();
            for (String category : catalogCategories) {
                for (WorkoutLogEntry entry : todayEntries) {
                    if (category.equals(entry.muscleGroup)) {
                        selectedToday.add(category);
                        break;
                    }
                }
            }

            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                boolean[] checked = new boolean[catalogCategories.size()];
                for (int i = 0; i < catalogCategories.size(); i++) {
                    checked[i] = selectedToday.contains(catalogCategories.get(i));
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Select categories for today")
                        .setMultiChoiceItems(catalogCategories.toArray(new CharSequence[0]), checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Next", (dialog, which) -> {
                            List<String> chosenCategories = new ArrayList<>();
                            for (int i = 0; i < catalogCategories.size(); i++) {
                                if (checked[i]) {
                                    chosenCategories.add(catalogCategories.get(i));
                                }
                            }
                            if (chosenCategories.isEmpty()) {
                                Toast.makeText(getContext(), "Select at least one category.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            showSelectExercisesDialog(chosenCategories);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void showSelectExercisesDialog(@NonNull List<String> chosenCategories) {
        LinkedHashMap<String, List<SeedExercise>> groupedOptions = new LinkedHashMap<>();
        for (SeedExercise exercise : DEFAULT_EXERCISES) {
            if (chosenCategories.contains(exercise.category)) {
                if (!groupedOptions.containsKey(exercise.category)) {
                    groupedOptions.put(exercise.category, new ArrayList<>());
                }
                groupedOptions.get(exercise.category).add(exercise);
            }
        }
        if (groupedOptions.isEmpty() || getContext() == null) {
            return;
        }

        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            List<WorkoutLogEntry> todayEntries = db.workoutLogDao().getByDay(today);
            List<String> selectedKeys = new ArrayList<>();
            for (WorkoutLogEntry entry : todayEntries) {
                selectedKeys.add(entry.muscleGroup + "|" + entry.exerciseName);
            }

            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                ScrollView scrollView = new ScrollView(requireContext());
                LinearLayout root = new LinearLayout(requireContext());
                root.setOrientation(LinearLayout.VERTICAL);
                int padding = dpToPx(18);
                root.setPadding(padding, padding, padding, padding);
                scrollView.addView(root);

                List<CheckBox> checkBoxes = new ArrayList<>();
                List<SeedExercise> exerciseOrder = new ArrayList<>();

                for (Map.Entry<String, List<SeedExercise>> section : groupedOptions.entrySet()) {
                    TextView heading = new TextView(requireContext());
                    heading.setText(section.getKey());
                    heading.setTextColor(0xFFFFFFFF);
                    heading.setTextSize(17f);
                    heading.setPadding(0, root.getChildCount() == 0 ? 0 : dpToPx(10), 0, dpToPx(6));
                    root.addView(heading);

                    for (SeedExercise exercise : section.getValue()) {
                        CheckBox box = new CheckBox(requireContext());
                        box.setText(exercise.name + " (" + describeSeedExercise(exercise) + ")");
                        box.setTextColor(0xFFFFFFFF);
                        box.setChecked(selectedKeys.contains(exercise.category + "|" + exercise.name));
                        root.addView(box);
                        checkBoxes.add(box);
                        exerciseOrder.add(exercise);
                    }
                }

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Select exercises for today")
                        .setView(scrollView)
                        .setPositiveButton("Apply plan", null)
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    List<SeedExercise> chosenExercises = new ArrayList<>();
                    for (int i = 0; i < checkBoxes.size(); i++) {
                        if (checkBoxes.get(i).isChecked()) {
                            chosenExercises.add(exerciseOrder.get(i));
                        }
                    }
                    if (chosenExercises.isEmpty()) {
                        Toast.makeText(getContext(), "Select at least one exercise.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    applyTodayPlan(chosenExercises);
                }));
                dialog.show();
            });
        });
    }

    private void applyTodayPlan(@NonNull List<SeedExercise> chosenExercises) {
        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            db.workoutLogDao().deleteByDay(today);
            for (SeedExercise exercise : chosenExercises) {
                WorkoutLogEntry entry = new WorkoutLogEntry();
                entry.dateDay = today;
                entry.muscleGroup = exercise.category;
                entry.exerciseName = exercise.name;
                entry.sets = exercise.sets;
                entry.reps = exercise.reps;
                entry.completed = false;
                db.workoutLogDao().insert(entry);
            }

            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                Toast.makeText(getContext(), "Today's workout plan updated.", Toast.LENGTH_SHORT).show();
                loadTodayWorkoutPlan();
            });
        });
    }

    private void renderHomeWorkoutGroups(@NonNull List<WorkoutLogEntry> todayEntries) {
        containerHomeWorkoutGroups.removeAllViews();
        if (getContext() == null) {
            return;
        }
        if (todayEntries.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No workout selected for today yet.");
            empty.setTextColor(0xCCFFFFFF);
            empty.setTextSize(14f);
            containerHomeWorkoutGroups.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        renderTodayWorkoutGroups(inflater, todayEntries);
    }

    private void renderTodayWorkoutGroups(@NonNull LayoutInflater inflater, @NonNull List<WorkoutLogEntry> todayEntries) {
        List<String> catalogCategories = getCatalogCategories();
        LinkedHashMap<String, List<WorkoutLogEntry>> groupedEntries = new LinkedHashMap<>();
        for (String category : catalogCategories) {
            for (WorkoutLogEntry entry : todayEntries) {
                if (category.equals(entry.muscleGroup)) {
                    if (!groupedEntries.containsKey(category)) {
                        groupedEntries.put(category, new ArrayList<>());
                    }
                    groupedEntries.get(category).add(entry);
                }
            }
        }
        for (WorkoutLogEntry entry : todayEntries) {
            if (!catalogCategories.contains(entry.muscleGroup)) {
                if (!groupedEntries.containsKey(entry.muscleGroup)) {
                    groupedEntries.put(entry.muscleGroup, new ArrayList<>());
                }
                groupedEntries.get(entry.muscleGroup).add(entry);
            }
        }

        for (Map.Entry<String, List<WorkoutLogEntry>> section : groupedEntries.entrySet()) {
            String group = section.getKey();
            List<WorkoutLogEntry> entries = section.getValue();
            View sectionView = inflater.inflate(R.layout.item_meal_section, containerHomeWorkoutGroups, false);
            TextView title = sectionView.findViewById(R.id.sectionTitle);
            TextView arrow = sectionView.findViewById(R.id.sectionArrow);
            LinearLayout content = sectionView.findViewById(R.id.sectionContent);
            View header = sectionView.findViewById(R.id.sectionHeader);

            boolean expanded = isWorkoutSectionExpanded(group);
            title.setText(group + " (" + entries.size() + ")");
            arrow.setText(expanded ? "▼" : "▶");
            content.setVisibility(expanded ? View.VISIBLE : View.GONE);

            for (WorkoutLogEntry entry : entries) {
                View row = inflater.inflate(R.layout.item_workout_exercise, content, false);
                CheckBox completed = row.findViewById(R.id.exerciseCompleted);
                TextView name = row.findViewById(R.id.exerciseName);
                TextView setsReps = row.findViewById(R.id.exerciseSetsReps);
                Button editBtn = row.findViewById(R.id.exerciseEdit);
                Button delBtn = row.findViewById(R.id.exerciseDelete);

                name.setText(entry.exerciseName);
                setsReps.setText(formatExerciseDetail(entry));
                completed.setChecked(entry.completed);
                editBtn.setVisibility(View.GONE);
                delBtn.setVisibility(View.GONE);

                WorkoutLogEntry currentEntry = entry;
                completed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    currentEntry.completed = isChecked;
                    AppExecutors.get().io().execute(() -> {
                        db.workoutLogDao().update(currentEntry);
                        Activity activity = getActivity();
                        if (activity == null || !isAdded()) {
                            return;
                        }
                        AppExecutors.get().main().execute(this::loadTodayWorkoutPlan);
                    });
                });
                content.addView(row);
            }

            header.setOnClickListener(view -> {
                boolean next = !isWorkoutSectionExpanded(group);
                workoutSectionExpanded.put(group, next);
                content.setVisibility(next ? View.VISIBLE : View.GONE);
                arrow.setText(next ? "▼" : "▶");
            });

            containerHomeWorkoutGroups.addView(sectionView);
        }
    }

    @NonNull
    private List<String> getCatalogCategories() {
        List<String> categories = new ArrayList<>();
        for (SeedExercise exercise : DEFAULT_EXERCISES) {
            if (!categories.contains(exercise.category)) {
                categories.add(exercise.category);
            }
        }
        return categories;
    }

    @NonNull
    private String describeSeedExercise(@NonNull SeedExercise exercise) {
        if ("Barbell Bench Press".equals(exercise.name)) return "4 sets x 8-10 reps";
        if ("Deadlifts".equals(exercise.name)) return "4 sets x 6-8 reps";
        if ("Plank".equals(exercise.name)) return "3 sets x 1 min";
        if ("Cardio (Treadmill / Cycling)".equals(exercise.name)) return "15-20 min";
        return exercise.sets + " sets x " + exercise.reps + " reps";
    }

    @NonNull
    private String formatExerciseDetail(@NonNull WorkoutLogEntry entry) {
        String name = entry.exerciseName != null ? entry.exerciseName : "";
        if ("Barbell Bench Press".equals(name)) return "4 sets x 8-10 reps";
        if ("Deadlifts".equals(name)) return "4 sets x 6-8 reps";
        if ("Plank".equals(name)) return "3 sets x 1 min";
        if ("Cardio (Treadmill / Cycling)".equals(name)) return "15-20 min";
        return entry.sets + " sets x " + entry.reps + " reps";
    }

    private boolean isWorkoutSectionExpanded(@NonNull String group) {
        Boolean expanded = workoutSectionExpanded.get(group);
        if (expanded == null) {
            workoutSectionExpanded.put(group, true);
            return true;
        }
        return expanded;
    }

    private void animateChakraProgress(@NonNull ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), targetProgress);
        animator.setDuration(650L);
        animator.start();
        progressBar.animate()
                .cancel();
        progressBar.setScaleY(1f);
        progressBar.animate()
                .scaleY(1.08f)
                .setDuration(180L)
                .withEndAction(() -> progressBar.animate().scaleY(1f).setDuration(220L).start())
                .start();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private float sumEatenCalories(List<MealEntry> meals) {
        float total = 0f;
        for (MealEntry meal : meals) {
            if (meal.eaten) {
                total += meal.calories;
            }
        }
        return total;
    }

    private static class SeedExercise {
        final String category;
        final String name;
        final int sets;
        final int reps;

        SeedExercise(String category, String name, int sets, int reps) {
            this.category = category;
            this.name = name;
            this.sets = sets;
            this.reps = reps;
        }
    }
}
