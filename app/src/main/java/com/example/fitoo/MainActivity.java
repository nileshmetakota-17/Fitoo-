package com.example.fitoo;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
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

    VideoView video;
    ImageView rasengan;
    BottomNavigationView bottomNav;
    String currentTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        setContentView(R.layout.activity_main);

        video = findViewById(R.id.videoBG);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chakra_video);
        video.setVideoURI(uri);
        video.start();
        video.setOnCompletionListener(mp -> video.start());

        rasengan = findViewById(R.id.rasengan);
        startRasenganAnimation();

        bottomNav = findViewById(R.id.bottomNav);

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

    @Override
    protected void onResume() {
        super.onResume();
        if (video != null && !video.isPlaying()) {
            video.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (video != null && video.isPlaying()) {
            video.pause();
        }
    }

    private void startRasenganAnimation() {
        rasengan.animate()
                .rotationBy(720)
                .setDuration(2000)
                .withEndAction(this::startRasenganAnimation)
                .start();
    }
}
