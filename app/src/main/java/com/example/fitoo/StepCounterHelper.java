package com.example.fitoo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Maps {@link android.hardware.Sensor#TYPE_STEP_COUNTER} (steps since boot) into
 * "steps today" using a per-day baseline stored when the calendar day changes.
 */
final class StepCounterHelper {

    private static final String PREFS = "fitoo_step_counter";
    private static final String KEY_DAY = "day_ymd";
    private static final String KEY_BASELINE = "baseline_total";

    private StepCounterHelper() {
    }

    static int todayStepsFromSensorTotal(Context ctx, long sensorTotal) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int ymd = ymd(Calendar.getInstance());
        int savedDay = p.getInt(KEY_DAY, -1);
        long baseline = p.getLong(KEY_BASELINE, 0L);
        SharedPreferences.Editor e = p.edit();
        if (savedDay != ymd) {
            e.putInt(KEY_DAY, ymd);
            e.putLong(KEY_BASELINE, sensorTotal);
            e.apply();
            return 0;
        }
        if (sensorTotal < baseline) {
            // Counter reset (e.g. reboot) — restart baseline
            e.putLong(KEY_BASELINE, sensorTotal);
            e.apply();
            return 0;
        }
        long delta = sensorTotal - baseline;
        if (delta > Integer.MAX_VALUE) {
            delta = Integer.MAX_VALUE;
        }
        return (int) delta;
    }

    private static int ymd(Calendar c) {
        return c.get(Calendar.YEAR) * 10000
                + (c.get(Calendar.MONTH) + 1) * 100
                + c.get(Calendar.DAY_OF_MONTH);
    }
}
