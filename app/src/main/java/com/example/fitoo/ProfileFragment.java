package com.example.fitoo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private ImageView profilePhoto;
    private LinearLayout photoActions;
    private LinearLayout profileSummary;
    private LinearLayout profileEditContainer;
    private TextView summaryName;
    private TextView summaryAge;
    private TextView summaryGender;
    private TextView summaryHeight;
    private TextView summaryWeight;
    private TextView summaryGoal;
    private TextView summaryTargetCalories;
    private TextView summaryTargetProtein;
    private EditText profileName;
    private EditText profileAge;
    private EditText profileHeight;
    private EditText profileWeight;
    private EditText profileTargetCalories;
    private EditText profileTargetProtein;
    private Spinner profileGender;
    private Spinner profileGoal;
    private Button btnEditProfile;
    private FitooDatabase db;

    private boolean hasLoadedOnce;
    private boolean isEditMode = true;
    private boolean profileExists;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    onPhotoPicked(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        db = FitooDatabase.get(requireContext());

        profilePhoto = v.findViewById(R.id.profilePhoto);
        photoActions = v.findViewById(R.id.photoActions);
        profileSummary = v.findViewById(R.id.profileSummary);
        profileEditContainer = v.findViewById(R.id.profileEditContainer);
        summaryName = v.findViewById(R.id.summaryName);
        summaryAge = v.findViewById(R.id.summaryAge);
        summaryGender = v.findViewById(R.id.summaryGender);
        summaryHeight = v.findViewById(R.id.summaryHeight);
        summaryWeight = v.findViewById(R.id.summaryWeight);
        summaryGoal = v.findViewById(R.id.summaryGoal);
        summaryTargetCalories = v.findViewById(R.id.summaryTargetCalories);
        summaryTargetProtein = v.findViewById(R.id.summaryTargetProtein);
        profileName = v.findViewById(R.id.profileName);
        profileAge = v.findViewById(R.id.profileAge);
        profileGender = v.findViewById(R.id.profileGender);
        profileHeight = v.findViewById(R.id.profileHeight);
        profileWeight = v.findViewById(R.id.profileWeight);
        profileTargetCalories = v.findViewById(R.id.profileTargetCalories);
        profileTargetProtein = v.findViewById(R.id.profileTargetProtein);
        profileGoal = v.findViewById(R.id.profileGoal);
        btnEditProfile = v.findViewById(R.id.btnEditProfile);
        Button btnChangePhoto = v.findViewById(R.id.btnChangePhoto);
        Button btnRemovePhoto = v.findViewById(R.id.btnRemovePhoto);
        Button btnSave = v.findViewById(R.id.btnSaveProfile);
        Button btnDeleteAllData = v.findViewById(R.id.btnDeleteAllData);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.genders, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.fitness_goals, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileGoal.setAdapter(goalAdapter);

        btnChangePhoto.setOnClickListener(view -> pickImageLauncher.launch("image/*"));
        btnRemovePhoto.setOnClickListener(view -> removePhoto());
        btnSave.setOnClickListener(view -> saveProfile());
        btnEditProfile.setOnClickListener(view -> setEditMode(true));
        btnDeleteAllData.setOnClickListener(view -> confirmDeleteAllDataStepOne());
        setupKeyboardAwareScroll(v);

        applyMode();
        loadProfile();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadProfile();
        }
    }

    public void refreshData() {
        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                UserProfile p = db.userProfileDao().get();
                runOnUiThreadSafe(() -> {
                    profileExists = p != null;
                    fillInputs(p);
                    fillSummary(p);
                    applyPhoto(p != null ? p.photoUri : null);

                    if (!hasLoadedOnce) {
                        hasLoadedOnce = true;
                        setEditMode(!profileExists);
                    } else {
                        applyMode();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load profile", e);
                runOnUiThreadSafe(() -> showToast("Could not load profile."));
            }
        }).start();
    }

    private void fillInputs(@Nullable UserProfile p) {
        profileName.setText(p != null && p.name != null ? p.name : "");
        profileAge.setText(p != null && p.age > 0 ? String.valueOf(p.age) : "");
        profileHeight.setText(p != null && p.heightCm > 0 ? String.valueOf(p.heightCm) : "");
        profileWeight.setText(p != null && p.weightKg > 0 ? String.valueOf(p.weightKg) : "");
        setSpinnerValue(profileGender, p != null ? p.gender : null);
        setSpinnerValue(profileGoal, p != null ? p.fitnessGoal : null);
        profileTargetCalories.setText(p != null && p.targetCalories > 0 ? trimFloat(p.targetCalories) : "");
        profileTargetProtein.setText(p != null && p.targetProtein > 0 ? trimFloat(p.targetProtein) : "");
    }

    private void fillSummary(@Nullable UserProfile p) {
        summaryName.setText("Name: " + valueOrDash(p != null ? p.name : null));
        summaryAge.setText("Age: " + (p != null && p.age > 0 ? p.age : "-"));
        summaryGender.setText("Gender: " + valueOrDash(p != null ? p.gender : null));
        summaryHeight.setText("Height: " + (p != null && p.heightCm > 0 ? trimFloat(p.heightCm) + " cm" : "-"));
        summaryWeight.setText("Weight: " + (p != null && p.weightKg > 0 ? trimFloat(p.weightKg) + " kg" : "-"));
        summaryGoal.setText("Goal: " + valueOrDash(p != null ? p.fitnessGoal : null));
        summaryTargetCalories.setText("Target calories: " + (p != null && p.targetCalories > 0 ? trimFloat(p.targetCalories) + " kcal" : "-"));
        summaryTargetProtein.setText("Target protein: " + (p != null && p.targetProtein > 0 ? trimFloat(p.targetProtein) + " g" : "-"));
    }

    private String valueOrDash(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    private String trimFloat(float value) {
        int whole = (int) value;
        if (whole == value) {
            return String.valueOf(whole);
        }
        return String.valueOf(value);
    }

    private void setSpinnerValue(@NonNull Spinner spinner, @Nullable String value) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && value.equals(item.toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void applyPhoto(@Nullable String photoUri) {
        if (photoUri == null || photoUri.trim().isEmpty()) {
            profilePhoto.setImageResource(R.drawable.ic_ninja);
            return;
        }
        try {
            profilePhoto.setImageURI(null);
            profilePhoto.setImageURI(Uri.parse(photoUri));
        } catch (Exception e) {
            profilePhoto.setImageResource(R.drawable.ic_ninja);
        }
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;
        applyMode();
    }

    private void applyMode() {
        boolean showSummary = profileExists && !isEditMode;
        profileSummary.setVisibility(showSummary ? View.VISIBLE : View.GONE);
        btnEditProfile.setVisibility(showSummary ? View.VISIBLE : View.GONE);
        profileEditContainer.setVisibility(showSummary ? View.GONE : View.VISIBLE);
        photoActions.setVisibility(showSummary ? View.GONE : View.VISIBLE);
    }

    private void saveProfile() {
        final String name = profileName.getText().toString().trim();
        String ageStr = profileAge.getText().toString().trim();
        String heightStr = profileHeight.getText().toString().trim();
        String weightStr = profileWeight.getText().toString().trim();
        String targetCaloriesStr = profileTargetCalories.getText().toString().trim();
        String targetProteinStr = profileTargetProtein.getText().toString().trim();
        final int age;
        final float height;
        final float weight;
        final float targetCalories;
        final float targetProtein;
        try {
            age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
            height = heightStr.isEmpty() ? 0 : Float.parseFloat(heightStr);
            weight = weightStr.isEmpty() ? 0 : Float.parseFloat(weightStr);
            targetCalories = targetCaloriesStr.isEmpty() ? 0 : Float.parseFloat(targetCaloriesStr);
            targetProtein = targetProteinStr.isEmpty() ? 0 : Float.parseFloat(targetProteinStr);
        } catch (NumberFormatException e) {
            showToast("Invalid numbers.");
            return;
        }

        Object goalItem = profileGoal.getSelectedItem();
        Object genderItem = profileGender.getSelectedItem();
        if (goalItem == null || genderItem == null) {
            showToast("Profile options are not ready yet.");
            return;
        }
        final String gender = genderItem.toString();
        final String goal = goalItem.toString();

        new Thread(() -> {
            try {
                UserProfile existing = db.userProfileDao().get();
                UserProfile p = new UserProfile();
                p.id = 1;
                p.name = name;
                p.gender = gender;
                p.age = age;
                p.heightCm = height;
                p.weightKg = weight;
                p.fitnessGoal = goal;
                p.targetCalories = targetCalories;
                p.targetProtein = targetProtein;
                p.photoUri = existing != null ? existing.photoUri : null;
                db.userProfileDao().insert(p);
                runOnUiThreadSafe(() -> {
                    showToast("Profile saved.");
                    profileExists = true;
                    setEditMode(false);
                    loadProfile();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to save profile", e);
                runOnUiThreadSafe(() -> showToast("Could not save profile."));
            }
        }).start();
    }

    private void removePhoto() {
        new Thread(() -> {
            try {
                UserProfile p = db.userProfileDao().get();
                if (p == null) {
                    p = new UserProfile();
                    p.id = 1;
                }
                p.photoUri = null;
                db.userProfileDao().insert(p);
                Context context = getContext();
                if (context != null) {
                    File avatarFile = new File(context.getFilesDir(), "profile_avatar.png");
                    if (avatarFile.exists()) {
                        avatarFile.delete();
                    }
                }
                runOnUiThreadSafe(() -> {
                    profilePhoto.setImageResource(R.drawable.ic_ninja);
                    showToast("Profile photo removed.");
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove photo", e);
                runOnUiThreadSafe(() -> showToast("Could not remove profile photo."));
            }
        }).start();
    }

    private void savePhotoUri(String uri) {
        new Thread(() -> {
            try {
                UserProfile p = db.userProfileDao().get();
                if (p == null) {
                    p = new UserProfile();
                    p.id = 1;
                }
                p.photoUri = uri;
                db.userProfileDao().insert(p);
                runOnUiThreadSafe(() -> profileExists = true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save photo URI", e);
                runOnUiThreadSafe(() -> showToast("Could not save profile photo."));
            }
        }).start();
    }

    private void setupKeyboardAwareScroll(@NonNull View root) {
        ScrollView profileScroll = root.findViewById(R.id.profileScroll);
        if (profileScroll == null) {
            return;
        }

        final int baseBottomPadding = profileScroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(profileScroll, (view, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View bottomNav = getActivity() != null ? getActivity().findViewById(R.id.bottomNav) : null;
            int appBottomNavHeight = bottomNav != null ? bottomNav.getHeight() : 0;
            int extraBottom = Math.max(0, imeInsets.bottom - systemInsets.bottom - appBottomNavHeight - dpToPx(8));
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    baseBottomPadding + extraBottom
            );

            if (extraBottom > 0) {
                View focused = view.findFocus();
                if (focused != null) {
                    view.post(() -> scrollFocusedIntoView(profileScroll, focused));
                }
            }
            return insets;
        });

        registerFocusScroll(profileScroll, profileName);
        registerFocusScroll(profileScroll, profileAge);
        registerFocusScroll(profileScroll, profileHeight);
        registerFocusScroll(profileScroll, profileWeight);
        registerFocusScroll(profileScroll, profileTargetCalories);
        registerFocusScroll(profileScroll, profileTargetProtein);
        ViewCompat.requestApplyInsets(profileScroll);
    }

    private void registerFocusScroll(@NonNull ScrollView scrollView, @NonNull View input) {
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollFocusedIntoView(scrollView, view), 150L);
            }
        });
    }

    private void scrollFocusedIntoView(@NonNull ScrollView scrollView, @NonNull View target) {
        int[] scrollLocation = new int[2];
        int[] targetLocation = new int[2];
        scrollView.getLocationOnScreen(scrollLocation);
        target.getLocationOnScreen(targetLocation);
        int targetTop = targetLocation[1] - scrollLocation[1] + scrollView.getScrollY();
        int desiredTop = Math.max(0, targetTop - dpToPx(20));
        scrollView.smoothScrollTo(0, desiredTop);
    }

    private int dpToPx(int dp) {
        Context context = getContext();
        if (context == null) {
            return dp;
        }
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private void onPhotoPicked(@Nullable Uri sourceUri) {
        if (sourceUri == null) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        new Thread(() -> {
            try {
                Bitmap source = decodeBitmap(context, sourceUri);
                if (source == null) {
                    throw new IllegalStateException("Could not decode selected image.");
                }
                runOnUiThreadSafe(() -> showCropDialog(source));
            } catch (Exception e) {
                Log.e(TAG, "Failed to update photo", e);
                runOnUiThreadSafe(() -> showToast("Photo update failed."));
            }
        }).start();
    }

    private void showCropDialog(@NonNull Bitmap source) {
        if (!isAdded() || getContext() == null) {
            source.recycle();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crop_photo, null);
        CircleCropImageView cropView = dialogView.findViewById(R.id.cropImageView);
        cropView.setBitmap(source);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Crop profile photo")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, w) -> {
                    Bitmap released = cropView.releaseBitmap();
                    if (released != null && !released.isRecycled()) {
                        released.recycle();
                    }
                })
                .setPositiveButton("Use photo", (d, w) -> {
                    Bitmap cropped = cropView.getCroppedBitmap();
                    Bitmap released = cropView.releaseBitmap();
                    if (released != null && released != cropped && !released.isRecycled()) {
                        released.recycle();
                    }
                    if (cropped == null) {
                        showToast("Could not crop image.");
                        return;
                    }
                    Context context = getContext();
                    if (context == null) {
                        cropped.recycle();
                        return;
                    }
                    new Thread(() -> {
                        try {
                            Uri savedUri = saveBitmapToInternalStorage(context, cropped);
                            runOnUiThreadSafe(() -> {
                                profilePhoto.setImageURI(null);
                                profilePhoto.setImageURI(savedUri);
                                showToast("Profile photo updated.");
                            });
                            savePhotoUri(savedUri.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save cropped photo", e);
                            runOnUiThreadSafe(() -> showToast("Photo update failed."));
                        }
                    }).start();
                })
                .create();
        dialog.setOnDismissListener(d -> {
            Bitmap released = cropView.releaseBitmap();
            if (released != null && !released.isRecycled()) {
                released.recycle();
            }
        });
        dialog.show();
    }

    @Nullable
    private Bitmap decodeBitmap(@NonNull Context context, @NonNull Uri uri) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                int width = info.getSize().getWidth();
                int height = info.getSize().getHeight();
                int maxDimension = Math.max(width, height);
                if (maxDimension > 1024) {
                    float scale = 1024f / maxDimension;
                    decoder.setTargetSize(
                            Math.max(1, Math.round(width * scale)),
                            Math.max(1, Math.round(height * scale))
                    );
                }
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        }

        int maxDimension = Math.max(bounds.outWidth, bounds.outHeight);
        int sampleSize = 1;
        while (maxDimension / sampleSize > 1024) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream, null, options);
        }
    }

    @NonNull
    private Uri saveBitmapToInternalStorage(@NonNull Context context, @NonNull Bitmap bitmap) throws Exception {
        File avatarFile = new File(context.getFilesDir(), "profile_avatar.png");
        try (FileOutputStream stream = new FileOutputStream(avatarFile, false)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new IllegalStateException("Could not save avatar bitmap.");
            }
        } finally {
            bitmap.recycle();
        }
        return Uri.fromFile(avatarFile);
    }

    private void runOnUiThreadSafe(@NonNull Runnable runnable) {
        Activity activity = getActivity();
        if (activity == null || !isAdded()) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            runnable.run();
        });
    }

    private void showToast(@NonNull String text) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteAllDataStepOne() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete all app data?")
                .setMessage("This will remove meals, workouts, profile, and chat settings.")
                .setPositiveButton("Continue", (d, w) -> confirmDeleteAllDataStepTwo())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteAllDataStepTwo() {
        if (getContext() == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Reconfirm delete")
                .setMessage("After this, app will reset like freshly installed. Delete now?")
                .setPositiveButton("Delete now", (d, w) -> clearAllAppData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllAppData() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        boolean cleared = false;
        try {
            ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                cleared = am.clearApplicationUserData();
            }
        } catch (Exception e) {
            Log.e(TAG, "clearApplicationUserData failed", e);
        }

        if (!cleared) {
            new Thread(() -> {
                try {
                    db.close();
                } catch (Exception ignored) {
                }
                Context context = getContext();
                if (context == null) {
                    return;
                }
                context.deleteDatabase("fitoo.db");
                context.deleteSharedPreferences(AiPreferences.PREFS_NAME);
                deleteRecursively(new File(context.getApplicationInfo().dataDir, "shared_prefs"));
                deleteRecursively(context.getFilesDir());
                deleteRecursively(context.getCacheDir());
                runOnUiThreadSafe(() -> showToast("Data cleared. Restart the app."));
            }).start();
        }
    }

    private void deleteRecursively(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
