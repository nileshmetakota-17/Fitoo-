package com.example.fitoo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String TAG_HOME = "home";
    private static final String TAG_MEALS = "meals";
    private static final String TAG_WORKOUTS = "workouts";
    private static final String TAG_SENSEI = "sensei";
    private static final String TAG_SETTINGS = "settings";

    BottomNavigationView bottomNav;
    String currentTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not rely on resize alone (often fails with bottom nav): pad nav host by IME inset instead.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        bottomNav = findViewById(R.id.bottomNav);
        setupKeyboardInsetsForNavHost();

        if (savedInstanceState == null) {
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.add(R.id.nav_host_container, new HomeFragment(), TAG_HOME);
            tx.add(R.id.nav_host_container, new MealsFragment(), TAG_MEALS);
            tx.add(R.id.nav_host_container, new WorkoutsFragment(), TAG_WORKOUTS);
            tx.add(R.id.nav_host_container, new SenseiFragment(), TAG_SENSEI);
            tx.add(R.id.nav_host_container, new ProfileFragment(), TAG_SETTINGS);
            tx.commitNow();
            FragmentTransaction tx2 = getSupportFragmentManager().beginTransaction();
            tx2.hide(getSupportFragmentManager().findFragmentByTag(TAG_MEALS));
            tx2.hide(getSupportFragmentManager().findFragmentByTag(TAG_WORKOUTS));
            tx2.hide(getSupportFragmentManager().findFragmentByTag(TAG_SENSEI));
            tx2.hide(getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS));
            tx2.commitNow();
            bottomNav.setSelectedItemId(R.id.nav_home);
            currentTag = TAG_HOME;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                currentTag = TAG_HOME;
                showFragment(TAG_HOME);
                return true;
            } else if (id == R.id.nav_meals) {
                currentTag = TAG_MEALS;
                showFragment(TAG_MEALS);
                return true;
            } else if (id == R.id.nav_workouts) {
                currentTag = TAG_WORKOUTS;
                showFragment(TAG_WORKOUTS);
                return true;
            } else if (id == R.id.nav_sensei) {
                currentTag = TAG_SENSEI;
                showFragment(TAG_SENSEI);
                return true;
            } else if (id == R.id.nav_settings) {
                currentTag = TAG_SETTINGS;
                showFragment(TAG_SETTINGS);
                return true;
            }
            return false;
        });
    }

    private void showFragment(String tag) {
        try {
            if (getSupportFragmentManager().isStateSaved()) {
                return;
            }
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.setReorderingAllowed(true);
            hideAllFragments(tx);
            Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
            if (f != null) {
                tx.show(f);
            }
            tx.commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch fragment to: " + tag, e);
            Toast.makeText(this, "Could not open this tab.", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideAllFragments(FragmentTransaction tx) {
        String[] tags = {TAG_HOME, TAG_MEALS, TAG_WORKOUTS, TAG_SENSEI, TAG_SETTINGS};
        for (String tag : tags) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
            if (f != null) {
                tx.hide(f);
            }
        }
    }

    /**
     * Pads the fragment area above the IME so Sensei (and other tabs) never draw the composer under the keyboard.
     * Hides bottom nav while the keyboard is open.
     */
    private void setupKeyboardInsetsForNavHost() {
        View mainRoot = findViewById(R.id.mainRoot);
        View navHost = findViewById(R.id.nav_host_container);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int imeBottom = ime.bottom;
            bottomNav.setVisibility(imeBottom > 0 ? View.GONE : View.VISIBLE);
            navHost.setPadding(0, 0, 0, imeBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(mainRoot);
    }
}
