package com.example.fitoo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class RoutineSessionActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTINE_ID = "routine_id";

    private int routineId;
    private String routineName = "Empty Workout";
    private final List<RoutineExercise> exercises = new ArrayList<>();
    private final List<CheckBox> checkboxes = new ArrayList<>();

    private TextView sessionTitle;
    private TextView sessionProgress;
    private LinearLayout containerExercises;
    private FitooDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_session);

        db = FitooDatabase.get(this);
        routineId = getIntent().getIntExtra(EXTRA_ROUTINE_ID, -1);

        sessionTitle = findViewById(R.id.sessionTitle);
        sessionProgress = findViewById(R.id.sessionProgress);
        containerExercises = findViewById(R.id.containerExercises);
        Button btnDone = findViewById(R.id.btnDone);

        if (routineId >= 0) {
            loadRoutine();
        } else {
            sessionTitle.setText("Empty Workout");
            sessionProgress.setText("Add exercises below");
            Button addBtn = new Button(this);
            addBtn.setText("+ Add Exercise");
            addBtn.setOnClickListener(v -> showAddExerciseDialog());
            containerExercises.addView(addBtn);
        }

        btnDone.setOnClickListener(v -> {
            Toast.makeText(this, "Routine complete! Chakra charged.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadRoutine() {
        new Thread(() -> {
            WorkoutRoutine r = db.workoutRoutineDao().getById(routineId);
            if (r == null) {
                runOnUiThread(this::finish);
                return;
            }
            routineName = r.name;
            List<RoutineExercise> list = db.routineExerciseDao().getByRoutineId(routineId);
            runOnUiThread(() -> {
                sessionTitle.setText("Complete: " + routineName);
                exercises.clear();
                exercises.addAll(list);
                checkboxes.clear();
                containerExercises.removeAllViews();
                LayoutInflater inflater = getLayoutInflater();
                for (RoutineExercise ex : exercises) {
                    View row = inflater.inflate(R.layout.item_exercise_session, containerExercises, false);
                    TextView name = row.findViewById(R.id.exerciseName);
                    TextView setsReps = row.findViewById(R.id.exerciseSetsReps);
                    CheckBox check = row.findViewById(R.id.checkDone);
                    name.setText(ex.name);
                    setsReps.setText(ex.setsReps != null ? ex.setsReps : "");
                    check.setOnCheckedChangeListener((b, checked) -> updateProgress());
                    checkboxes.add(check);
                    containerExercises.addView(row);
                }
                updateProgress();
            });
        }).start();
    }

    private void updateProgress() {
        int done = 0;
        for (CheckBox c : checkboxes) if (c.isChecked()) done++;
        int total = checkboxes.size();
        sessionProgress.setText("Challenges: " + done + "/" + total);
    }

    private void showAddExerciseDialog() {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("Exercise name");
        android.widget.EditText setsInput = new android.widget.EditText(this);
        setsInput.setHint("Sets x Reps (e.g. 3x12)");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(nameInput);
        layout.addView(setsInput);
        new AlertDialog.Builder(this)
                .setTitle("Add Exercise")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    String sets = setsInput.getText().toString().trim();
                    if (name.isEmpty()) return;
                    RoutineExercise ex = new RoutineExercise();
                    ex.name = name;
                    ex.setsReps = sets;
                    ex.routineId = -1;
                    ex.sortOrder = exercises.size();
                    exercises.add(ex);
                    View row = getLayoutInflater().inflate(R.layout.item_exercise_session, containerExercises, false);
                    TextView nameTv = row.findViewById(R.id.exerciseName);
                    TextView setsTv = row.findViewById(R.id.exerciseSetsReps);
                    CheckBox check = row.findViewById(R.id.checkDone);
                    nameTv.setText(ex.name);
                    setsTv.setText(ex.setsReps);
                    check.setOnCheckedChangeListener((b, checked) -> updateProgress());
                    checkboxes.add(check);
                    containerExercises.addView(row, containerExercises.getChildCount() - 1);
                    updateProgress();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
