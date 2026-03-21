package com.example.fitoo;

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
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MealsFragment extends Fragment {

    private static final String[] CATEGORIES = {"Breakfast", "Lunch", "Snacks", "Dinner"};

    private TextView txtTotalCalories, txtTotalProtein, txtTotalCarbs, txtTotalFats, txtTotalFiber;
    private TextView txtMealProgressLabel;
    private ProgressBar mealProgressBar;
    private Spinner spinnerSort;
    private Button btnAddMeal, btnApplyDietPlan, btnManageDietPlan;
    private LinearLayout containerSections;
    private AlertDialog manageDietPlanDialog;

    private FitooDatabase db;
    private int sortMode = 0;
    private final boolean[] sectionExpanded = {true, true, true, true};
    private int lastInsertedMealId = -1;
    private boolean isMealOperationInProgress;
    private AlertDialog activeMealDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_meals, container, false);
        db = FitooDatabase.get(requireContext());

        txtTotalCalories = v.findViewById(R.id.txtTotalCalories);
        txtTotalProtein = v.findViewById(R.id.txtTotalProtein);
        txtTotalCarbs = v.findViewById(R.id.txtTotalCarbs);
        txtTotalFats = v.findViewById(R.id.txtTotalFats);
        txtTotalFiber = v.findViewById(R.id.txtTotalFiber);
        txtMealProgressLabel = v.findViewById(R.id.txtMealProgressLabel);
        mealProgressBar = v.findViewById(R.id.mealProgressBar);
        spinnerSort = v.findViewById(R.id.spinnerSort);
        btnAddMeal = v.findViewById(R.id.btnAddMeal);
        btnApplyDietPlan = v.findViewById(R.id.btnApplyDietPlan);
        btnManageDietPlan = v.findViewById(R.id.btnManageDietPlan);
        containerSections = v.findViewById(R.id.containerSections);

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.meal_sort_options,
                android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setSelection(0);
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                sortMode = position;
                refreshSummariesAndList();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnAddMeal.setOnClickListener(view -> showAddOrEditMealDialog(null));
        btnApplyDietPlan.setOnClickListener(view -> applyDietPlanToToday());
        btnManageDietPlan.setOnClickListener(view -> showManageDietPlanDialog());

        refreshSummariesAndList();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSummariesAndList();
    }

    @Override
    public void onDestroyView() {
        if (manageDietPlanDialog != null && manageDietPlanDialog.isShowing()) {
            manageDietPlanDialog.dismiss();
        }
        manageDietPlanDialog = null;
        super.onDestroyView();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshSummariesAndList();
        }
    }

    public void refreshData() {
        refreshSummariesAndList();
    }

    private static final String UNKNOWN_FOOD_MSG = "Food not recognized. Try grams unit for online lookup, or enter macros manually.";

    private void showAddOrEditMealDialog(MealEntry existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_meal, null);
        EditText inputName = dialogView.findViewById(R.id.dialog_meal_name);
        EditText inputQty = dialogView.findViewById(R.id.dialog_meal_quantity);
        EditText inputCalories = dialogView.findViewById(R.id.dialog_meal_calories);
        EditText inputProtein = dialogView.findViewById(R.id.dialog_meal_protein);
        EditText inputCarbs = dialogView.findViewById(R.id.dialog_meal_carbs);
        EditText inputFats = dialogView.findViewById(R.id.dialog_meal_fats);
        EditText inputFiber = dialogView.findViewById(R.id.dialog_meal_fiber);
        Spinner spinnerUnit = dialogView.findViewById(R.id.dialog_meal_unit);
        Spinner spinnerCat = dialogView.findViewById(R.id.dialog_meal_category);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.meal_unit,
                android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.meal_categories,
                android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        if (existing != null) {
            inputName.setText(existing.name);
            inputQty.setText(String.valueOf(existing.quantity));
            spinnerUnit.setSelection("g".equals(existing.quantityUnit) ? 1 : 0);
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(existing.category)) {
                    spinnerCat.setSelection(i);
                    break;
                }
            }
            inputCalories.setText(trimFloat(existing.calories));
            inputProtein.setText(trimFloat(existing.protein));
            inputCarbs.setText(trimFloat(existing.carbs));
            inputFats.setText(trimFloat(existing.fats));
            inputFiber.setText(trimFloat(existing.fiber));
        }

        boolean isEdit = existing != null;
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "Edit Food Item" : "Add Food Item")
                .setView(dialogView)
                .setPositiveButton(isEdit ? "Save" : "Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            activeMealDialog = dialog;
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negative != null) {
                negative.setEnabled(!isMealOperationInProgress);
                negative.setAlpha(isMealOperationInProgress ? 0.45f : 1f);
            }
            positive.setOnClickListener(v -> {
                if (isMealOperationInProgress) {
                    return;
                }
                String name = inputName.getText().toString().trim();
                String qtyStr = inputQty.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Enter food name.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (TextUtils.isEmpty(qtyStr)) {
                    Toast.makeText(getContext(), "Enter amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
                float qty;
                try {
                    qty = Float.parseFloat(qtyStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount. Use a number.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (qty <= 0) {
                    Toast.makeText(getContext(), "Amount must be greater than 0.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isWeightGrams = spinnerUnit.getSelectedItemPosition() == 1;
                Float manualCalories = parseOptionalFloat(inputCalories);
                Float manualProtein = parseOptionalFloat(inputProtein);
                Float manualCarbs = parseOptionalFloat(inputCarbs);
                Float manualFats = parseOptionalFloat(inputFats);
                Float manualFiber = parseOptionalFloat(inputFiber);

                if (manualCalories == null && !inputCalories.getText().toString().trim().isEmpty()
                        || manualProtein == null && !inputProtein.getText().toString().trim().isEmpty()
                        || manualCarbs == null && !inputCarbs.getText().toString().trim().isEmpty()
                        || manualFats == null && !inputFats.getText().toString().trim().isEmpty()
                        || manualFiber == null && !inputFiber.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Invalid macro value.", Toast.LENGTH_SHORT).show();
                    return;
                }

                NutritionLookup.MacroInfo info = NutritionLookup.estimate(name, qty, isWeightGrams);
                boolean hasManualOverride = manualCalories != null
                        || manualProtein != null
                        || manualCarbs != null
                        || manualFats != null
                        || manualFiber != null;

                if (info == null && !hasManualOverride) {
                    if (!isWeightGrams) {
                        Toast.makeText(getContext(), UNKNOWN_FOOD_MSG, Toast.LENGTH_LONG).show();
                        return;
                    }
                    positive.setEnabled(false);
                    setMealUiBusy(true);
                    Toast.makeText(getContext(), "Searching nutrition online...", Toast.LENGTH_SHORT).show();
                    float grams = qty;
                    AppExecutors.get().io().execute(() -> {
                        NutritionLookup.MacroInfo fetched = null;
                        String error = null;
                        try {
                            fetched = OnlineNutritionLookup.lookupPerGrams(name, grams);
                        } catch (Exception e) {
                            error = e.getMessage();
                        }
                        NutritionLookup.MacroInfo finalFetched = fetched;
                        String finalError = error;
                        AppExecutors.get().main().execute(() -> {
                            if (!isAdded()) {
                                return;
                            }
                            positive.setEnabled(true);
                            setMealUiBusy(false);
                            if (finalFetched == null) {
                                String msg = "Could not fetch nutrition online. Enter macros manually.";
                                if (finalError != null && !finalError.trim().isEmpty()) {
                                    msg = msg + " (" + finalError + ")";
                                }
                                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                                return;
                            }
                            saveMealFromInfo(existing, isEdit, name, qty, isWeightGrams, finalFetched, dialog);
                        });
                    });
                    return;
                }

                if (info == null) {
                    info = new NutritionLookup.MacroInfo();
                }
                if (manualCalories != null) info.calories = manualCalories;
                if (manualProtein != null) info.protein = manualProtein;
                if (manualCarbs != null) info.carbs = manualCarbs;
                if (manualFats != null) info.fats = manualFats;
                if (manualFiber != null) info.fiber = manualFiber;

                saveMealFromInfo(existing, isEdit, name, qty, isWeightGrams, info, dialog);
            });
        });
        dialog.setOnDismissListener(d -> {
            if (activeMealDialog == dialog) {
                activeMealDialog = null;
            }
        });

        dialog.show();
    }

    private void saveMealFromInfo(@Nullable MealEntry existing,
                                  boolean isEdit,
                                  @NonNull String name,
                                  float qty,
                                  boolean isWeightGrams,
                                  @NonNull NutritionLookup.MacroInfo info,
                                  @NonNull AlertDialog dialogToDismiss) {
        String category = "Breakfast";
        Spinner spinnerCat = dialogToDismiss.findViewById(R.id.dialog_meal_category);
        if (spinnerCat != null && spinnerCat.getSelectedItem() != null) {
            category = spinnerCat.getSelectedItem().toString();
        }
        String unit = isWeightGrams ? "g" : "count";

        if (isEdit && existing != null) {
            setMealUiBusy(true);
            existing.name = name;
            existing.category = category;
            existing.quantity = qty;
            existing.quantityUnit = unit;
            existing.calories = info.calories;
            existing.protein = info.protein;
            existing.carbs = info.carbs;
            existing.fats = info.fats;
            existing.fiber = info.fiber;
            AppExecutors.get().io().execute(() -> {
                db.mealDao().updateMeal(existing);
                AppExecutors.get().main().execute(() -> {
                    Toast.makeText(getContext(), "Updated.", Toast.LENGTH_SHORT).show();
                    refreshSummariesAndList();
                    dialogToDismiss.dismiss();
                    setMealUiBusy(false);
                });
            });
            return;
        }

        MealEntry meal = new MealEntry();
        meal.name = name;
        meal.category = category;
        meal.quantity = qty;
        meal.quantityUnit = unit;
        meal.calories = info.calories;
        meal.protein = info.protein;
        meal.carbs = info.carbs;
        meal.fats = info.fats;
        meal.fiber = info.fiber;
        meal.timestamp = System.currentTimeMillis();
        meal.eaten = false;
        setMealUiBusy(true);
        AppExecutors.get().io().execute(() -> {
            long id = db.mealDao().insertMeal(meal);
            int insertedId = (int) id;
            AppExecutors.get().main().execute(() -> {
                Toast.makeText(getContext(), "Food added.", Toast.LENGTH_SHORT).show();
                lastInsertedMealId = insertedId;
                refreshSummariesAndList();
                dialogToDismiss.dismiss();
                setMealUiBusy(false);
            });
        });
    }

    private void setMealUiBusy(boolean busy) {
        isMealOperationInProgress = busy;
        if (!isAdded() || getActivity() == null) {
            return;
        }
        View bottomNav = getActivity().findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setEnabled(!busy);
            bottomNav.setAlpha(busy ? 0.45f : 1f);
        }
        if (btnAddMeal != null) btnAddMeal.setEnabled(!busy);
        if (btnApplyDietPlan != null) btnApplyDietPlan.setEnabled(!busy);
        if (btnManageDietPlan != null) btnManageDietPlan.setEnabled(!busy);
        if (spinnerSort != null) spinnerSort.setEnabled(!busy);

        if (activeMealDialog != null && activeMealDialog.isShowing()) {
            Button cancel = activeMealDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (cancel != null) {
                cancel.setEnabled(!busy);
                cancel.setAlpha(busy ? 0.45f : 1f);
            }
        }
    }

    private void applyDietPlanToToday() {
        AppExecutors.get().io().execute(() -> {
            List<DietPlanItem> plan = db.dietPlanDao().getAll();
            long startToday = startOfDay(System.currentTimeMillis());
            for (DietPlanItem item : plan) {
                MealEntry meal = new MealEntry();
                meal.name = item.name;
                meal.category = item.category;
                meal.quantity = item.quantity;
                meal.quantityUnit = item.quantityUnit != null ? item.quantityUnit : "count";
                meal.calories = item.calories;
                meal.protein = item.protein;
                meal.carbs = item.carbs;
                meal.fats = item.fats;
                meal.fiber = item.fiber;
                meal.timestamp = startToday + (meal.hashCode() & 0xFFFF);
                meal.eaten = false;
                db.mealDao().insertMeal(meal);
            }
            AppExecutors.get().main().execute(() -> {
                Toast.makeText(getContext(), "Diet plan applied to today.", Toast.LENGTH_SHORT).show();
                refreshSummariesAndList();
            });
        });
    }

    private void showManageDietPlanDialog() {
        AppExecutors.get().io().execute(() -> {
            List<DietPlanItem> plan = db.dietPlanDao().getAll();
            AppExecutors.get().main().execute(() -> {
                if (manageDietPlanDialog != null && manageDietPlanDialog.isShowing()) {
                    manageDietPlanDialog.dismiss();
                }
                LinearLayout list = new LinearLayout(requireContext());
                list.setOrientation(LinearLayout.VERTICAL);
                for (DietPlanItem item : plan) {
                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(30, 16, 30, 16);

                    TextView label = new TextView(requireContext());
                    label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    label.setText(item.name + " — " + Math.round(item.calories) + " kcal");
                    label.setTextColor(0xFFFFFFFF);

                    Button removeBtn = new Button(requireContext());
                    removeBtn.setText("Remove");
                    removeBtn.setOnClickListener(v2 -> AppExecutors.get().io().execute(() -> {
                        db.dietPlanDao().delete(item);
                        AppExecutors.get().main().execute(() -> {
                            Toast.makeText(getContext(), "Removed from plan.", Toast.LENGTH_SHORT).show();
                            showManageDietPlanDialog();
                        });
                    }));

                    row.addView(label);
                    row.addView(removeBtn);
                    list.addView(row);
                }
                Button addBtn = new Button(requireContext());
                addBtn.setText("Add to plan");
                addBtn.setOnClickListener(v2 -> {
                    if (manageDietPlanDialog != null && manageDietPlanDialog.isShowing()) {
                        manageDietPlanDialog.dismiss();
                    }
                    showAddToDietPlanDialog();
                });
                list.addView(addBtn);
                android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
                scroll.addView(list);
                manageDietPlanDialog = new AlertDialog.Builder(requireContext())
                        .setTitle("My Diet Plan")
                        .setView(scroll)
                        .setNegativeButton("Close", null)
                        .show();
                manageDietPlanDialog.setOnDismissListener(d -> manageDietPlanDialog = null);
            });
        });
    }

    private void showAddToDietPlanDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_meal, null);
        EditText inputName = dialogView.findViewById(R.id.dialog_meal_name);
        EditText inputQty = dialogView.findViewById(R.id.dialog_meal_quantity);
        EditText inputCalories = dialogView.findViewById(R.id.dialog_meal_calories);
        EditText inputProtein = dialogView.findViewById(R.id.dialog_meal_protein);
        EditText inputCarbs = dialogView.findViewById(R.id.dialog_meal_carbs);
        EditText inputFats = dialogView.findViewById(R.id.dialog_meal_fats);
        EditText inputFiber = dialogView.findViewById(R.id.dialog_meal_fiber);
        Spinner spinnerUnit = dialogView.findViewById(R.id.dialog_meal_unit);
        Spinner spinnerCat = dialogView.findViewById(R.id.dialog_meal_category);
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.meal_unit,
                android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.meal_categories,
                android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add to Diet Plan")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            activeMealDialog = dialog;
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negative != null) {
                negative.setEnabled(!isMealOperationInProgress);
                negative.setAlpha(isMealOperationInProgress ? 0.45f : 1f);
            }
            positive.setOnClickListener(v -> {
                if (isMealOperationInProgress) {
                    return;
                }
                String name = inputName.getText().toString().trim();
                String qtyStr = inputQty.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Enter food name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(qtyStr)) {
                    Toast.makeText(getContext(), "Enter amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
                float qty;
                try {
                    qty = Float.parseFloat(qtyStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (qty <= 0) {
                    Toast.makeText(getContext(), "Amount must be > 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean isWeightGrams = spinnerUnit.getSelectedItemPosition() == 1;
                Float manualCalories = parseOptionalFloat(inputCalories);
                Float manualProtein = parseOptionalFloat(inputProtein);
                Float manualCarbs = parseOptionalFloat(inputCarbs);
                Float manualFats = parseOptionalFloat(inputFats);
                Float manualFiber = parseOptionalFloat(inputFiber);

                if (manualCalories == null && !inputCalories.getText().toString().trim().isEmpty()
                        || manualProtein == null && !inputProtein.getText().toString().trim().isEmpty()
                        || manualCarbs == null && !inputCarbs.getText().toString().trim().isEmpty()
                        || manualFats == null && !inputFats.getText().toString().trim().isEmpty()
                        || manualFiber == null && !inputFiber.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "Invalid macro value.", Toast.LENGTH_SHORT).show();
                    return;
                }

                NutritionLookup.MacroInfo info = NutritionLookup.estimate(name, qty, isWeightGrams);
                boolean hasManualOverride = manualCalories != null
                        || manualProtein != null
                        || manualCarbs != null
                        || manualFats != null
                        || manualFiber != null;

                if (info == null && !hasManualOverride) {
                    if (!isWeightGrams) {
                        Toast.makeText(getContext(), UNKNOWN_FOOD_MSG, Toast.LENGTH_LONG).show();
                        return;
                    }
                    positive.setEnabled(false);
                    setMealUiBusy(true);
                    Toast.makeText(getContext(), "Searching nutrition online...", Toast.LENGTH_SHORT).show();
                    float grams = qty;
                    AppExecutors.get().io().execute(() -> {
                        NutritionLookup.MacroInfo fetched = null;
                        String error = null;
                        try {
                            fetched = OnlineNutritionLookup.lookupPerGrams(name, grams);
                        } catch (Exception e) {
                            error = e.getMessage();
                        }
                        NutritionLookup.MacroInfo finalFetched = fetched;
                        String finalError = error;
                        AppExecutors.get().main().execute(() -> {
                            if (!isAdded()) {
                                return;
                            }
                            positive.setEnabled(true);
                            setMealUiBusy(false);
                            if (finalFetched == null) {
                                String msg = "Could not fetch nutrition online. Enter macros manually.";
                                if (finalError != null && !finalError.trim().isEmpty()) {
                                    msg = msg + " (" + finalError + ")";
                                }
                                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                                return;
                            }
                            saveDietPlanItemFromInfo(name, qty, isWeightGrams, finalFetched, dialog);
                        });
                    });
                    return;
                }

                if (info == null) {
                    info = new NutritionLookup.MacroInfo();
                }
                if (manualCalories != null) info.calories = manualCalories;
                if (manualProtein != null) info.protein = manualProtein;
                if (manualCarbs != null) info.carbs = manualCarbs;
                if (manualFats != null) info.fats = manualFats;
                if (manualFiber != null) info.fiber = manualFiber;

                saveDietPlanItemFromInfo(name, qty, isWeightGrams, info, dialog);
            });
        });
        dialog.setOnDismissListener(d -> {
            if (activeMealDialog == dialog) {
                activeMealDialog = null;
            }
        });

        dialog.show();
    }

    private void saveDietPlanItemFromInfo(@NonNull String name,
                                          float qty,
                                          boolean isWeightGrams,
                                          @NonNull NutritionLookup.MacroInfo info,
                                          @NonNull AlertDialog dialogToDismiss) {
        setMealUiBusy(true);
        String category = "Breakfast";
        Spinner spinnerCat = dialogToDismiss.findViewById(R.id.dialog_meal_category);
        if (spinnerCat != null && spinnerCat.getSelectedItem() != null) {
            category = spinnerCat.getSelectedItem().toString();
        }

        DietPlanItem item = new DietPlanItem();
        item.name = name;
        item.category = category;
        item.quantity = qty;
        item.quantityUnit = isWeightGrams ? "g" : "count";
        item.calories = info.calories;
        item.protein = info.protein;
        item.carbs = info.carbs;
        item.fats = info.fats;
        item.fiber = info.fiber;
        item.sortOrder = 0;
        AppExecutors.get().io().execute(() -> {
            db.dietPlanDao().insert(item);
            AppExecutors.get().main().execute(() -> {
                Toast.makeText(getContext(), "Added to plan.", Toast.LENGTH_SHORT).show();
                dialogToDismiss.dismiss();
                showManageDietPlanDialog();
                setMealUiBusy(false);
            });
        });
    }

    private void refreshSummariesAndList() {
        AppExecutors.get().io().execute(() -> {
            long startToday = startOfDay(System.currentTimeMillis());
            long endToday = endOfDay(System.currentTimeMillis());

            List<MealEntry> all = db.mealDao().getMealsBetween(startToday, endToday);
            if (sortMode == 1) Collections.sort(all, (a, b) -> Float.compare(b.calories, a.calories));
            else if (sortMode == 0) Collections.sort(all, (a, b) -> Long.compare(b.timestamp, a.timestamp));

            float eatenCalories = 0f;
            float eatenProtein = 0f;
            float eatenCarbs = 0f;
            float eatenFats = 0f;
            float eatenFiber = 0f;
            int eatenCount = 0;
            for (MealEntry m : all) {
                if (m.eaten) {
                    eatenCount++;
                    eatenCalories += m.calories;
                    eatenProtein += m.protein;
                    eatenCarbs += m.carbs;
                    eatenFats += m.fats;
                    eatenFiber += m.fiber;
                }
            }
            final int total = all.size();
            final int finalEatenCount = eatenCount;
            final int progress = total > 0 ? (finalEatenCount * 100 / total) : 0;
            final float finalEatenCalories = eatenCalories;
            final float finalEatenProtein = eatenProtein;
            final float finalEatenCarbs = eatenCarbs;
            final float finalEatenFats = eatenFats;
            final float finalEatenFiber = eatenFiber;
            UserProfile profile = db.userProfileDao().get();
            final float calorieTarget = profile != null ? profile.targetCalories : 0f;
            final float proteinTarget = profile != null ? profile.targetProtein : 0f;

            AppExecutors.get().main().execute(() -> {
                txtTotalCalories.setText(buildMacroLine("Calories", finalEatenCalories, "kcal", calorieTarget));
                txtTotalProtein.setText(buildMacroLine("Protein", finalEatenProtein, "g", proteinTarget));
                txtTotalCarbs.setText("Carbs: " + Math.round(finalEatenCarbs) + " g");
                txtTotalFats.setText("Fats: " + Math.round(finalEatenFats) + " g");
                txtTotalFiber.setText("Fiber: " + Math.round(finalEatenFiber) + " g");
                txtMealProgressLabel.setText("Meals eaten: " + finalEatenCount + "/" + total);
                mealProgressBar.setProgress(progress);
                renderSections(all, finalEatenCount, total);
            });
        });
    }

    private void renderSections(List<MealEntry> all, int eatenCount, int total) {
        containerSections.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            final int index = i;
            List<MealEntry> forCategory = new ArrayList<>();
            for (MealEntry m : all) {
                if (cat.equals(m.category)) forCategory.add(m);
            }

            View sectionView = inflater.inflate(R.layout.item_meal_section, containerSections, false);
            TextView title = sectionView.findViewById(R.id.sectionTitle);
            TextView arrow = sectionView.findViewById(R.id.sectionArrow);
            LinearLayout content = sectionView.findViewById(R.id.sectionContent);
            View header = sectionView.findViewById(R.id.sectionHeader);

            title.setText(cat + " (" + forCategory.size() + ")");
            content.setVisibility(sectionExpanded[index] ? View.VISIBLE : View.GONE);
            arrow.setText(sectionExpanded[index] ? "▼" : "▶");

            for (MealEntry m : forCategory) {
                View row = inflater.inflate(R.layout.item_meal_food, content, false);
                CheckBox eaten = row.findViewById(R.id.foodEaten);
                TextView name = row.findViewById(R.id.foodName);
                TextView macros = row.findViewById(R.id.foodMacros);
                Button editBtn = row.findViewById(R.id.foodEdit);
                Button delBtn = row.findViewById(R.id.foodDelete);

                String unitStr = "g".equals(m.quantityUnit) ? " g" : " pcs";
                name.setText(m.name + " · " + m.quantity + unitStr);
                macros.setText(Math.round(m.calories) + " kcal  P:" + Math.round(m.protein) + "g C:" + Math.round(m.carbs) + "g F:" + Math.round(m.fats) + "g Fi:" + Math.round(m.fiber) + "g");
                eaten.setChecked(m.eaten);

                if (m.id == lastInsertedMealId) {
                    lastInsertedMealId = -1;
                    row.setAlpha(0f);
                    row.setTranslationY(18f);
                    row.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(220L)
                            .withEndAction(() -> row.animate().alpha(0.92f).setDuration(90L).withEndAction(() -> row.animate().alpha(1f).setDuration(140L).start()).start())
                            .start();
                }

                MealEntry meal = m;
                eaten.setOnCheckedChangeListener((b, checked) -> {
                    meal.eaten = checked;
                    AppExecutors.get().io().execute(() -> {
                        db.mealDao().updateMeal(meal);
                        AppExecutors.get().main().execute(this::refreshSummariesAndList);
                    });
                });
                editBtn.setOnClickListener(v -> showAddOrEditMealDialog(meal));
                delBtn.setOnClickListener(v -> {
                    AppExecutors.get().io().execute(() -> {
                        db.mealDao().deleteById(meal.id);
                        AppExecutors.get().main().execute(this::refreshSummariesAndList);
                    });
                });
                content.addView(row);
            }

            header.setOnClickListener(v -> {
                sectionExpanded[index] = !sectionExpanded[index];
                content.setVisibility(sectionExpanded[index] ? View.VISIBLE : View.GONE);
                arrow.setText(sectionExpanded[index] ? "▼" : "▶");
            });
            containerSections.addView(sectionView);
        }
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

    @Nullable
    private Float parseOptionalFloat(@NonNull EditText input) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildMacroLine(@NonNull String label, float eaten, @NonNull String unit, float target) {
        if (target > 0f) {
            int percent = Math.max(0, Math.min(999, Math.round((eaten / target) * 100f)));
            return label + ": " + Math.round(eaten) + " " + unit + " (" + percent + "% of " + Math.round(target) + " " + unit + ")";
        }
        return label + ": " + Math.round(eaten) + " " + unit;
    }

    private String trimFloat(float value) {
        int whole = (int) value;
        if (whole == value) {
            return String.valueOf(whole);
        }
        return String.valueOf(value);
    }
}
