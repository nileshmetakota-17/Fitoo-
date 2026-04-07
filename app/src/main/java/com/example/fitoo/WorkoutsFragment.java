package com.example.fitoo;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutsFragment extends Fragment {

    private static final String PREFS_NAME = "fitoo_workout_prefs";
    private static final String KEY_MUSCLE_GROUPS = "muscle_groups";
    private static final String KEY_HIDDEN_DEFAULT_EXERCISES = "hidden_default_exercises";
    private static final String CATEGORY_SEPARATOR = "||";
    private static final String EXERCISE_KEY_SEPARATOR = "##";
    private static final String[] DEFAULT_MUSCLE_GROUPS = {
            "Chest", "Back", "Triceps", "Biceps", "Shoulders", "Legs","Core"
    };
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

    private TextView txtWorkoutProgressLabel;
    private ProgressBar workoutProgressBar;
    private LinearLayout containerMuscleGroups;
    private LinearLayout containerHistory;
    private FitooDatabase db;
    private final List<String> muscleGroups = new ArrayList<>();
    private final Map<String, Boolean> sectionExpanded = new HashMap<>();
    private final java.util.HashSet<String> hiddenDefaultExercises = new java.util.HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_workouts, container, false);

        db = FitooDatabase.get(requireContext());
        loadMuscleGroups();
        loadHiddenDefaultExercises();
        normalizeLegacyOtherCategory();

        txtWorkoutProgressLabel = v.findViewById(R.id.txtWorkoutProgressLabel);
        workoutProgressBar = v.findViewById(R.id.workoutProgressBar);
        containerMuscleGroups = v.findViewById(R.id.containerMuscleGroups);
        containerHistory = v.findViewById(R.id.containerHistory);
        Button btnAdd = v.findViewById(R.id.btnAddExercise);
        Button btnManageCategories = v.findViewById(R.id.btnManageCategories);
        Button btnManageDefaults = v.findViewById(R.id.btnManageDefaults);

        btnAdd.setOnClickListener(view -> showAddExerciseDialog());
        btnManageCategories.setOnClickListener(view -> showManageCategoriesDialog());
        if (btnManageDefaults != null) {
            btnManageDefaults.setOnClickListener(view -> showManageDefaultsDialog());
        }

        loadTodayAndHistory();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMuscleGroups();
        loadHiddenDefaultExercises();
        loadTodayAndHistory();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadMuscleGroups();
            loadHiddenDefaultExercises();
            loadTodayAndHistory();
        }
    }

    public void refreshData() {
        loadMuscleGroups();
        loadTodayAndHistory();
    }

    private void showAddExerciseDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_workout, null);
        Spinner spinnerGroup = dialogView.findViewById(R.id.dialog_muscle_group);
        EditText nameInput = dialogView.findViewById(R.id.dialog_exercise_name);
        EditText setsInput = dialogView.findViewById(R.id.dialog_sets);
        EditText repsInput = dialogView.findViewById(R.id.dialog_reps);

        spinnerGroup.setAdapter(createGroupAdapter());

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Exercise")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    String setsStr = setsInput.getText().toString().trim();
                    String repsStr = repsInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Enter exercise name.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sets = 0;
                    int reps = 0;
                    try {
                        if (!setsStr.isEmpty()) sets = Integer.parseInt(setsStr);
                        if (!repsStr.isEmpty()) reps = Integer.parseInt(repsStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid sets/reps.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Object selected = spinnerGroup.getSelectedItem();
                    if (selected == null) {
                        Toast.makeText(getContext(), "Add at least one category first.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String group = selected.toString();
                    long today = startOfDay(System.currentTimeMillis());

                    WorkoutLogEntry entry = new WorkoutLogEntry();
                    entry.dateDay = today;
                    entry.muscleGroup = group;
                    entry.exerciseName = name;
                    entry.sets = sets;
                    entry.reps = reps;
                    entry.completed = false;

                    new Thread(() -> {
                        db.workoutLogDao().insert(entry);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Exercise added.", Toast.LENGTH_SHORT).show();
                            loadTodayAndHistory();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTodayAndHistory() {
        new Thread(() -> {
            long today = startOfDay(System.currentTimeMillis());
            List<WorkoutLogEntry> todayEntries = db.workoutLogDao().getByDay(today);
            List<Long> recentDays = db.workoutLogDao().getRecentDays();
            int[] counts = new int[recentDays.size()];
            for (int i = 0; i < recentDays.size(); i++) {
                counts[i] = db.workoutLogDao().getByDay(recentDays.get(i)).size();
            }
            int completed = 0;
            for (WorkoutLogEntry e : todayEntries) {
                if (e.completed) {
                    completed++;
                }
            }
            final int total = todayEntries.size();
            final int finalCompleted = completed;
            final int progress = total > 0 ? (finalCompleted * 100 / total) : 0;

            requireActivity().runOnUiThread(() -> {
                txtWorkoutProgressLabel.setText("Completed: " + finalCompleted + "/" + total);
                animateProgress(workoutProgressBar, progress);
                renderMuscleGroups(todayEntries);
                renderHistory(recentDays, counts);
            });
        }).start();
    }

    private void showEditExerciseDialog(WorkoutLogEntry entry) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_workout, null);
        Spinner spinnerGroup = dialogView.findViewById(R.id.dialog_muscle_group);
        EditText nameInput = dialogView.findViewById(R.id.dialog_exercise_name);
        EditText setsInput = dialogView.findViewById(R.id.dialog_sets);
        EditText repsInput = dialogView.findViewById(R.id.dialog_reps);

        spinnerGroup.setAdapter(createGroupAdapter());
        nameInput.setText(entry.exerciseName);
        setsInput.setText(String.valueOf(entry.sets));
        repsInput.setText(String.valueOf(entry.reps));

        int currentIndex = muscleGroups.indexOf(entry.muscleGroup);
        if (currentIndex >= 0) {
            spinnerGroup.setSelection(currentIndex);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    int sets = 0;
                    int reps = 0;
                    try {
                        sets = Integer.parseInt(setsInput.getText().toString());
                        reps = Integer.parseInt(repsInput.getText().toString());
                    } catch (NumberFormatException ignored) {
                    }
                    entry.exerciseName = name;
                    Object selected = spinnerGroup.getSelectedItem();
                    entry.muscleGroup = selected != null ? selected.toString() : entry.muscleGroup;
                    entry.sets = sets;
                    entry.reps = reps;
                    new Thread(() -> {
                        db.workoutLogDao().update(entry);
                        requireActivity().runOnUiThread(this::loadTodayAndHistory);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderMuscleGroups(List<WorkoutLogEntry> todayEntries) {
        containerMuscleGroups.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        Map<String, WorkoutLogEntry> todayEntryMap = new HashMap<>();
        List<String> groupsToRender = new ArrayList<>(muscleGroups);
        for (SeedExercise exercise : DEFAULT_EXERCISES) {
            if (!groupsToRender.contains(exercise.category)) {
                groupsToRender.add(exercise.category);
            }
        }
        for (WorkoutLogEntry entry : todayEntries) {
            todayEntryMap.put(entry.muscleGroup + "|" + entry.exerciseName, entry);
            if (!groupsToRender.contains(entry.muscleGroup)) {
                groupsToRender.add(entry.muscleGroup);
            }
        }

        for (String group : groupsToRender) {
            List<SeedExercise> defaultForGroup = new ArrayList<>();
            List<WorkoutLogEntry> customForGroup = new ArrayList<>();
            for (SeedExercise exercise : DEFAULT_EXERCISES) {
                if (group.equals(exercise.category) && !isDefaultExerciseHidden(exercise)) {
                    defaultForGroup.add(exercise);
                }
            }
            for (WorkoutLogEntry entry : todayEntries) {
                if (group.equals(entry.muscleGroup) && !containsSeedExercise(defaultForGroup, entry.exerciseName)) {
                    customForGroup.add(entry);
                }
            }
            if (defaultForGroup.isEmpty() && customForGroup.isEmpty()) {
                continue;
            }

            View sectionView = inflater.inflate(R.layout.item_meal_section, containerMuscleGroups, false);
            TextView title = sectionView.findViewById(R.id.sectionTitle);
            TextView arrow = sectionView.findViewById(R.id.sectionArrow);
            LinearLayout content = sectionView.findViewById(R.id.sectionContent);
            View header = sectionView.findViewById(R.id.sectionHeader);

            boolean expanded = isSectionExpanded(group);
            title.setText(group + " (" + (defaultForGroup.size() + customForGroup.size()) + ")");
            content.setVisibility(expanded ? View.VISIBLE : View.GONE);
            arrow.setText(expanded ? "▼" : "▶");

            for (SeedExercise exercise : defaultForGroup) {
                View row = inflater.inflate(R.layout.item_workout_exercise, content, false);
                CheckBox completed = row.findViewById(R.id.exerciseCompleted);
                TextView name = row.findViewById(R.id.exerciseName);
                TextView setsReps = row.findViewById(R.id.exerciseSetsReps);
                Button editBtn = row.findViewById(R.id.exerciseEdit);
                Button delBtn = row.findViewById(R.id.exerciseDelete);
                name.setText(exercise.name);
                setsReps.setText(describeSeedExercise(exercise));
                editBtn.setVisibility(View.GONE);
                delBtn.setVisibility(View.VISIBLE);

                WorkoutLogEntry entry = todayEntryMap.get(group + "|" + exercise.name);
                completed.setAlpha(1f);
                row.setAlpha(1f);
                completed.setEnabled(true);

                if (entry == null) {
                    completed.setChecked(false);
                    completed.setOnCheckedChangeListener((b, checked) -> {
                        // First interaction with a default exercise should create today's entry.
                        AppExecutors.get().io().execute(() -> {
                            long today = startOfDay(System.currentTimeMillis());
                            WorkoutLogEntry created = new WorkoutLogEntry();
                            created.dateDay = today;
                            created.muscleGroup = group;
                            created.exerciseName = exercise.name;
                            created.sets = exercise.sets;
                            created.reps = exercise.reps;
                            created.completed = checked;
                            db.workoutLogDao().insert(created);
                            AppExecutors.get().main().execute(this::loadTodayAndHistory);
                        });
                    });
                    delBtn.setOnClickListener(v -> confirmHideDefaultExercise(exercise));
                } else {
                    completed.setChecked(entry.completed);
                    WorkoutLogEntry currentEntry = entry;
                    completed.setOnCheckedChangeListener((b, checked) -> {
                        currentEntry.completed = checked;
                        AppExecutors.get().io().execute(() -> {
                            db.workoutLogDao().update(currentEntry);
                            AppExecutors.get().main().execute(this::loadTodayAndHistory);
                        });
                    });
                    editBtn.setVisibility(View.VISIBLE);
                    delBtn.setVisibility(View.VISIBLE);
                    editBtn.setOnClickListener(v -> showEditExerciseDialog(currentEntry));
                    delBtn.setOnClickListener(v -> confirmHideDefaultExercise(exercise));
                }
                content.addView(row);
            }

            for (WorkoutLogEntry entry : customForGroup) {
                View row = inflater.inflate(R.layout.item_workout_exercise, content, false);
                CheckBox completed = row.findViewById(R.id.exerciseCompleted);
                TextView name = row.findViewById(R.id.exerciseName);
                TextView setsReps = row.findViewById(R.id.exerciseSetsReps);
                Button editBtn = row.findViewById(R.id.exerciseEdit);
                Button delBtn = row.findViewById(R.id.exerciseDelete);
                name.setText(entry.exerciseName);
                setsReps.setText(formatExerciseDetail(entry));
                completed.setChecked(entry.completed);

                WorkoutLogEntry currentEntry = entry;
                completed.setOnCheckedChangeListener((b, checked) -> {
                    currentEntry.completed = checked;
                    new Thread(() -> {
                        db.workoutLogDao().update(currentEntry);
                        requireActivity().runOnUiThread(this::loadTodayAndHistory);
                    }).start();
                });
                editBtn.setOnClickListener(v -> showEditExerciseDialog(currentEntry));
                delBtn.setOnClickListener(v -> {
                    new Thread(() -> {
                        db.workoutLogDao().deleteById(currentEntry.id);
                        requireActivity().runOnUiThread(this::loadTodayAndHistory);
                    }).start();
                });
                content.addView(row);
            }

            header.setOnClickListener(v -> {
                boolean next = !isSectionExpanded(group);
                sectionExpanded.put(group, next);
                content.setVisibility(next ? View.VISIBLE : View.GONE);
                arrow.setText(next ? "▼" : "▶");
            });

            containerMuscleGroups.addView(sectionView);
        }
    }

    private boolean containsSeedExercise(@NonNull List<SeedExercise> exercises, @NonNull String name) {
        for (SeedExercise exercise : exercises) {
            if (name.equals(exercise.name)) {
                return true;
            }
        }
        return false;
    }

    private void renderHistory(List<Long> recentDays, int[] counts) {
        containerHistory.removeAllViews();
        if (recentDays == null || recentDays.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No workout history yet.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            containerHistory.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        for (int i = 0; i < recentDays.size(); i++) {
            Long day = recentDays.get(i);
            int count = i < counts.length ? counts[i] : 0;
            View row = inflater.inflate(android.R.layout.simple_list_item_2, containerHistory, false);
            TextView t1 = row.findViewById(android.R.id.text1);
            TextView t2 = row.findViewById(android.R.id.text2);
            t1.setText(sdf.format(day));
            t1.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            t2.setText(count + " exercise(s)");
            t2.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            containerHistory.addView(row);
        }
    }

    private void showManageCategoriesDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 16, 24, 16);

        EditText input = new EditText(context);
        input.setHint("New category name");
        root.addView(input);

        Button addButton = new Button(context);
        addButton.setText("Add category");
        root.addView(addButton);

        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        scroll.addView(listContainer);
        root.addView(scroll);

        Runnable refreshRows = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                for (String group : muscleGroups) {
                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 10, 0, 10);

                    TextView label = new TextView(context);
                    label.setText(group);
                    label.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                    label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    Button remove = new Button(context);
                    remove.setText("Remove");
                    remove.setEnabled(muscleGroups.size() > 1);
                    remove.setOnClickListener(v -> {
                        if (muscleGroups.size() <= 1) {
                            Toast.makeText(context, "At least one category is required.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AlertDialog.Builder(context)
                                .setTitle("Remove category?")
                                .setMessage("Exercises in \"" + group + "\" will be moved to another category.")
                                .setPositiveButton("Remove", (d, w) -> removeCategoryAndReassign(group, this))
                                .setNegativeButton("Cancel", null)
                                .show();
                    });

                    row.addView(label);
                    row.addView(remove);
                    listContainer.addView(row);
                }
            }
        };

        addButton.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(context, "Enter category name.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String existing : muscleGroups) {
                if (existing.equalsIgnoreCase(value)) {
                    Toast.makeText(context, "Category already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            muscleGroups.add(value);
            sectionExpanded.put(value, true);
            saveMuscleGroups();
            loadTodayAndHistory();
            input.setText("");
            refreshRows.run();
        });

        refreshRows.run();
        new AlertDialog.Builder(context)
                .setTitle("Workout categories")
                .setView(root)
                .setPositiveButton("Done", null)
                .show();
    }

    private void removeCategoryAndReassign(@NonNull String removedCategory, @NonNull Runnable onDoneUiThread) {
        String replacement;
        if ("General".equals(removedCategory)) {
            replacement = null;
            for (String group : muscleGroups) {
                if (!removedCategory.equals(group)) {
                    replacement = group;
                    break;
                }
            }
            if (replacement == null) {
                replacement = "General";
                if (!muscleGroups.contains(replacement)) {
                    muscleGroups.add(replacement);
                }
            }
        } else {
            replacement = "General";
            if (!muscleGroups.contains(replacement)) {
                muscleGroups.add(replacement);
            }
        }

        muscleGroups.remove(removedCategory);
        if (muscleGroups.isEmpty()) {
            muscleGroups.add("General");
        }
        sectionExpanded.remove(removedCategory);
        saveMuscleGroups();

        String finalReplacement = replacement;
        new Thread(() -> {
            db.workoutLogDao().replaceMuscleGroup(removedCategory, finalReplacement);
            requireActivity().runOnUiThread(() -> {
                loadTodayAndHistory();
                onDoneUiThread.run();
            });
        }).start();
    }

    private void loadMuscleGroups() {
        muscleGroups.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MUSCLE_GROUPS, "");
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split("\\Q" + CATEGORY_SEPARATOR + "\\E");
            for (String part : parts) {
                String value = part.trim();
                if ("Other".equalsIgnoreCase(value)) {
                    value = "General";
                }
                if (!value.isEmpty()) {
                    boolean exists = false;
                    for (String existing : muscleGroups) {
                        if (existing.equalsIgnoreCase(value)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        muscleGroups.add(value);
                    }
                }
            }
        }

        if (muscleGroups.isEmpty()) {
            for (String value : DEFAULT_MUSCLE_GROUPS) {
                muscleGroups.add(value);
            }
        }
        saveMuscleGroups();
    }

    private void saveMuscleGroups() {
        String encoded = TextUtils.join(CATEGORY_SEPARATOR, muscleGroups);
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MUSCLE_GROUPS, encoded)
                .apply();
    }

    private void loadHiddenDefaultExercises() {
        hiddenDefaultExercises.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_HIDDEN_DEFAULT_EXERCISES, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\Q" + CATEGORY_SEPARATOR + "\\E");
        for (String part : parts) {
            String v = part.trim();
            if (!v.isEmpty()) {
                hiddenDefaultExercises.add(v);
            }
        }
    }

    private void saveHiddenDefaultExercises() {
        String encoded = TextUtils.join(CATEGORY_SEPARATOR, hiddenDefaultExercises);
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HIDDEN_DEFAULT_EXERCISES, encoded)
                .apply();
    }

    private boolean isDefaultExerciseHidden(@NonNull SeedExercise ex) {
        return hiddenDefaultExercises.contains(ex.category + EXERCISE_KEY_SEPARATOR + ex.name);
    }

    private void confirmHideDefaultExercise(@NonNull SeedExercise ex) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("Remove exercise?")
                .setMessage("This will hide \"" + ex.name + "\" from the default list. You can restore it later.")
                .setPositiveButton("Remove", (d, w) -> hideDefaultExercise(ex))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideDefaultExercise(@NonNull SeedExercise ex) {
        hiddenDefaultExercises.add(ex.category + EXERCISE_KEY_SEPARATOR + ex.name);
        saveHiddenDefaultExercises();
        // Also remove any existing entry for today so it disappears immediately.
        AppExecutors.get().io().execute(() -> {
            long today = startOfDay(System.currentTimeMillis());
            List<WorkoutLogEntry> todayEntries = db.workoutLogDao().getByDay(today);
            for (WorkoutLogEntry entry : todayEntries) {
                if (ex.category.equals(entry.muscleGroup) && ex.name.equals(entry.exerciseName)) {
                    db.workoutLogDao().deleteById(entry.id);
                }
            }
            AppExecutors.get().main().execute(() -> {
                Toast.makeText(getContext(), "Removed.", Toast.LENGTH_SHORT).show();
                loadTodayAndHistory();
            });
        });
    }

    private void showManageDefaultsDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("Default exercises")
                .setMessage("Restore all hidden default exercises?")
                .setPositiveButton("Restore", (d, w) -> {
                    hiddenDefaultExercises.clear();
                    saveHiddenDefaultExercises();
                    loadTodayAndHistory();
                    Toast.makeText(getContext(), "Defaults restored.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    @NonNull
    private String describeSeedExercise(@NonNull SeedExercise exercise) {
        if ("Barbell Bench Press".equals(exercise.name)) return "4 sets x 8-10 reps";
        if ("Deadlifts".equals(exercise.name)) return "4 sets x 6-8 reps";
        if ("Plank".equals(exercise.name)) return "3 sets x 1 min";
        if ("Cardio (Treadmill / Cycling)".equals(exercise.name)) return "15-20 min";
        return exercise.sets + " sets x " + exercise.reps + " reps";
    }

    private void animateProgress(@NonNull ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), targetProgress);
        animator.setDuration(350L);
        animator.start();
    }

    private void normalizeLegacyOtherCategory() {
        new Thread(() -> {
            db.workoutLogDao().replaceMuscleGroup("Other", "General");
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(this::loadTodayAndHistory);
            }
        }).start();
    }

    private ArrayAdapter<String> createGroupAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                muscleGroups
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private boolean isSectionExpanded(@NonNull String group) {
        Boolean expanded = sectionExpanded.get(group);
        if (expanded == null) {
            sectionExpanded.put(group, true);
            return true;
        }
        return expanded;
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
