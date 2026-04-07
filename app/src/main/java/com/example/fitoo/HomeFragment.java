package com.example.fitoo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment implements SensorEventListener {

    /** Daily goals for ring fill (steps / active minutes / move kcal estimates). */
    private static final int ACTIVITY_STEP_GOAL = 10_000;
    private static final int ACTIVITY_MIN_GOAL = 30;
    private static final int ACTIVITY_KCAL_GOAL = 300;
    /** Rough walking pace for minutes-from-steps estimate. */
    private static final float STEPS_PER_ACTIVE_MINUTE = 110f;
    private static final float KCAL_PER_STEP_ESTIMATE = 0.045f;

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
    private TextView txtStepsNumber;
    private TextView txtActiveMinsValue;
    private TextView txtMoveKcalValue;
    private TextView txtStepsHint;
    private Button btnStepsPermission;
    private ActivityRingsView activityRings;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private ActivityResultLauncher<String> requestStepsPermission;

    private FitooDatabase db;
    private final Map<String, Boolean> workoutSectionExpanded = new LinkedHashMap<>();
    private boolean showTodayWorkoutOnly = true;
    /** Drops stale async results when {@link #loadTodayWorkoutPlan()} is called again. */
    private final AtomicInteger todayWorkoutLoadGeneration = new AtomicInteger();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestStepsPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isAdded()) {
                        return;
                    }
                    refreshStepsAfterPermission(granted);
                });
    }

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
        txtStepsNumber = v.findViewById(R.id.txtStepsNumber);
        txtActiveMinsValue = v.findViewById(R.id.txtActiveMinsValue);
        txtMoveKcalValue = v.findViewById(R.id.txtMoveKcalValue);
        txtStepsHint = v.findViewById(R.id.txtStepsHint);
        btnStepsPermission = v.findViewById(R.id.btnStepsPermission);
        activityRings = v.findViewById(R.id.activityRings);
        btnRestartReports = v.findViewById(R.id.btnRestartReports);
        Button btnSelectHomeWorkoutPlan = v.findViewById(R.id.btnSelectHomeWorkoutPlan);
        btnRestartReports.setOnClickListener(view -> showResetDialog());
        btnSelectHomeWorkoutPlan.setOnClickListener(view -> showSelectTodayPlanDialog());
        switchShowTodayWorkoutOnly.setChecked(showTodayWorkoutOnly);
        switchShowTodayWorkoutOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showTodayWorkoutOnly = isChecked;
            loadTodayWorkoutPlan();
        });

        btnStepsPermission.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestStepsPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        });

        loadDashboard();
        loadTodayWorkoutPlan();
        tryRegisterStepsOrShowPermissionUi();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
        loadTodayWorkoutPlan();
        tryRegisterStepsOrShowPermissionUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterStepSensor();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadDashboard();
            loadTodayWorkoutPlan();
            tryRegisterStepsOrShowPermissionUi();
        } else {
            unregisterStepSensor();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER || txtStepsNumber == null || getContext() == null) {
            return;
        }
        long total = (long) event.values[0];
        int steps = StepCounterHelper.todayStepsFromSensorTotal(requireContext(), total);
        applyActivityMetrics(steps);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void refreshStepsAfterPermission(boolean granted) {
        if (btnStepsPermission != null) {
            btnStepsPermission.setVisibility(granted ? View.GONE : View.VISIBLE);
        }
        if (granted) {
            if (txtStepsHint != null) {
                txtStepsHint.setText(R.string.steps_hint);
            }
            tryRegisterStepSensor();
        } else {
            clearActivityMetricsUi(R.string.steps_permission_denied);
        }
    }

    private boolean needsActivityRecognitionPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private boolean hasActivityRecognitionPermission() {
        Context ctx = getContext();
        if (ctx == null) {
            return false;
        }
        if (!needsActivityRecognitionPermission()) {
            return true;
        }
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void tryRegisterStepsOrShowPermissionUi() {
        if (txtStepsNumber == null) {
            return;
        }
        if (needsActivityRecognitionPermission() && !hasActivityRecognitionPermission()) {
            btnStepsPermission.setVisibility(View.VISIBLE);
            clearActivityMetricsUi(R.string.steps_tap_allow);
            return;
        }
        btnStepsPermission.setVisibility(View.GONE);
        txtStepsHint.setText(R.string.steps_hint);
        tryRegisterStepSensor();
    }

    private void clearActivityMetricsUi(@StringRes int hintRes) {
        txtStepsNumber.setText("—");
        txtActiveMinsValue.setText("—");
        txtMoveKcalValue.setText("—");
        if (activityRings != null) {
            activityRings.setProgress(0f, 0f, 0f);
        }
        txtStepsHint.setText(hintRes);
    }

    private void applyActivityMetrics(int steps) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        txtStepsNumber.setText(nf.format(Math.max(0, steps)));
        int mins = Math.max(0, Math.round(steps / STEPS_PER_ACTIVE_MINUTE));
        int kcal = Math.max(0, Math.round(steps * KCAL_PER_STEP_ESTIMATE));
        txtActiveMinsValue.setText(String.valueOf(mins));
        txtMoveKcalValue.setText(String.valueOf(kcal));
        if (activityRings != null) {
            float pSteps = Math.min(1f, steps / (float) ACTIVITY_STEP_GOAL);
            float pMins = Math.min(1f, mins / (float) ACTIVITY_MIN_GOAL);
            float pKcal = Math.min(1f, kcal / (float) ACTIVITY_KCAL_GOAL);
            activityRings.setProgress(pSteps, pMins, pKcal);
        }
    }

    private void tryRegisterStepSensor() {
        Context ctx = getContext();
        if (ctx == null || !isAdded()) {
            return;
        }
        if (needsActivityRecognitionPermission() && !hasActivityRecognitionPermission()) {
            return;
        }
        if (sensorManager == null) {
            sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager == null) {
            txtStepsNumber.setText(R.string.steps_unavailable);
            txtActiveMinsValue.setText("—");
            txtMoveKcalValue.setText("—");
            if (activityRings != null) {
                activityRings.setProgress(0f, 0f, 0f);
            }
            return;
        }
        if (stepSensor == null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        if (stepSensor == null) {
            txtStepsNumber.setText(R.string.steps_unavailable);
            txtActiveMinsValue.setText("—");
            txtMoveKcalValue.setText("—");
            if (activityRings != null) {
                activityRings.setProgress(0f, 0f, 0f);
            }
            return;
        }
        sensorManager.unregisterListener(this);
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI, new Handler(Looper.getMainLooper()));
    }

    private void unregisterStepSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
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
        final int generation = todayWorkoutLoadGeneration.incrementAndGet();
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
                if (!isAdded() || generation != todayWorkoutLoadGeneration.get()) {
                    return;
                }
                if (!showTodayWorkoutOnly) {
                    txtHomeWorkoutProgressLabel.setText("Turn on the toggle to show today's workout.");
                    txtHomeWorkoutProgressLabel.setVisibility(View.VISIBLE);
                    homeWorkoutProgressBar.setVisibility(View.GONE);
                    containerHomeWorkoutGroups.removeAllViews();
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

    /** Removes every exercise scheduled for today (empty plan). */
    private void clearTodayWorkoutPlan() {
        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            db.workoutLogDao().deleteByDay(today);
            Activity activity = getActivity();
            if (activity == null || !isAdded()) {
                return;
            }
            AppExecutors.get().main().execute(() -> {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Today's workout cleared.", Toast.LENGTH_SHORT).show();
                loadTodayWorkoutPlan();
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
                            dialog.dismiss();
                            if (chosenCategories.isEmpty()) {
                                clearTodayWorkoutPlan();
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
                    heading.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    heading.setTextSize(17f);
                    heading.setPadding(0, root.getChildCount() == 0 ? 0 : dpToPx(10), 0, dpToPx(6));
                    root.addView(heading);

                    for (SeedExercise exercise : section.getValue()) {
                        CheckBox box = new CheckBox(requireContext());
                        box.setText(exercise.name + " (" + describeSeedExercise(exercise) + ")");
                        box.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
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
                    dialog.dismiss();
                    if (chosenExercises.isEmpty()) {
                        clearTodayWorkoutPlan();
                        return;
                    }
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
            empty.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
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
