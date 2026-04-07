package com.example.fitoo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Three concentric circular rings (steps / active mins / move kcal) with independent progress 0–1.
 */
public class ActivityRingsView extends View {

    private static final float START_DEG = -90f;

    private float progressSteps = 0f;
    private float progressMins = 0f;
    private float progressCal = 0f;

    private final Paint paintTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public ActivityRingsView(Context context) {
        super(context);
        init();
    }

    public ActivityRingsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActivityRingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintTrack.setStyle(Paint.Style.STROKE);
        paintTrack.setStrokeCap(Paint.Cap.ROUND);

        paintFill.setStyle(Paint.Style.STROKE);
        paintFill.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float steps01, float mins01, float cal01) {
        progressSteps = clamp01(steps01);
        progressMins = clamp01(mins01);
        progressCal = clamp01(cal01);
        invalidate();
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float minSide = Math.min(w, h);
        float density = getResources().getDisplayMetrics().density;

        float strokeOuter = 5.5f * density;
        float strokeMid = 4.5f * density;
        float strokeInner = 4f * density;

        float pad = 2f * density;
        float outerR = minSide / 2f - pad - strokeOuter / 2f;
        float midR = outerR * 0.72f;
        float innerR = outerR * 0.52f;

        int cTrack = 0x33FFFFFF;
        int cSteps = Color.parseColor("#4CAF50");
        int cMins = Color.parseColor("#2196F3");
        int cCal = Color.parseColor("#E91E63");

        drawRing(canvas, cx, cy, outerR, strokeOuter, cTrack, cSteps, progressSteps);
        drawRing(canvas, cx, cy, midR, strokeMid, cTrack, cMins, progressMins);
        drawRing(canvas, cx, cy, innerR, strokeInner, cTrack, cCal, progressCal);
    }

    private void drawRing(Canvas canvas, float cx, float cy, float radius,
                          float strokePx, int trackColor, int fillColor, float progress) {
        float left = cx - radius;
        float top = cy - radius;
        float right = cx + radius;
        float bottom = cy + radius;
        oval.set(left, top, right, bottom);

        paintTrack.setColor(trackColor);
        paintTrack.setStrokeWidth(strokePx);
        canvas.drawArc(oval, START_DEG, 360f, false, paintTrack);

        float sweep = 360f * progress;
        if (sweep > 0.05f) {
            paintFill.setColor(fillColor);
            paintFill.setStrokeWidth(strokePx);
            canvas.drawArc(oval, START_DEG, sweep, false, paintFill);
        }
    }
}
