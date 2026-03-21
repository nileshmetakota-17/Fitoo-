package com.example.fitoo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CircleCropImageView extends AppCompatImageView {

    private final Matrix imageMatrixInternal = new Matrix();
    private final float[] matrixValues = new float[9];
    private final RectF bitmapRect = new RectF();
    private final RectF mappedRect = new RectF();
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private final float overlayPaddingPx;

    private Bitmap sourceBitmap;
    private float minScale = 1f;
    private float maxScale = 5f;
    private float lastX;
    private float lastY;
    private boolean dragging;

    public CircleCropImageView(@NonNull Context context) {
        this(context, null);
    }

    public CircleCropImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleCropImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        overlayPaddingPx = 16f * context.getResources().getDisplayMetrics().density;

        overlayPaint.setColor(0x66000000);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * context.getResources().getDisplayMetrics().density);
        borderPaint.setColor(0xFFFFFFFF);
    }

    public void setBitmap(@NonNull Bitmap bitmap) {
        sourceBitmap = bitmap;
        setImageBitmap(bitmap);
        fitImageToCropArea();
    }

    @Nullable
    public Bitmap releaseBitmap() {
        Bitmap released = sourceBitmap;
        sourceBitmap = null;
        setImageDrawable(null);
        return released;
    }

    @Nullable
    public Bitmap getCroppedBitmap() {
        if (sourceBitmap == null || sourceBitmap.isRecycled()) {
            return null;
        }

        float diameterF = getCropDiameter();
        int diameter = Math.max(1, Math.round(diameterF));
        float cropLeft = (getWidth() - diameterF) / 2f;
        float cropTop = (getHeight() - diameterF) / 2f;

        Bitmap output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        BitmapShader shader = new BitmapShader(sourceBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix shaderMatrix = new Matrix(imageMatrixInternal);
        shaderMatrix.postTranslate(-cropLeft, -cropTop);
        shader.setLocalMatrix(shaderMatrix);
        paint.setShader(shader);

        float radius = diameter / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitImageToCropArea();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sourceBitmap == null) {
            return false;
        }

        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && dragging) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    imageMatrixInternal.postTranslate(dx, dy);
                    clampToCropArea();
                    applyImageMatrix();
                    lastX = event.getX();
                    lastY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float diameter = getCropDiameter();
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = diameter / 2f;

        Path overlayPath = new Path();
        overlayPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CCW);
        overlayPath.addCircle(cx, cy, radius, Path.Direction.CW);
        overlayPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(overlayPath, overlayPaint);
        canvas.drawCircle(cx, cy, radius, borderPaint);
    }

    private void fitImageToCropArea() {
        if (sourceBitmap == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        float cropDiameter = getCropDiameter();
        float scale = Math.max(
                cropDiameter / sourceBitmap.getWidth(),
                cropDiameter / sourceBitmap.getHeight()
        );
        minScale = scale;

        imageMatrixInternal.reset();
        imageMatrixInternal.postScale(scale, scale);
        float dx = (getWidth() - sourceBitmap.getWidth() * scale) / 2f;
        float dy = (getHeight() - sourceBitmap.getHeight() * scale) / 2f;
        imageMatrixInternal.postTranslate(dx, dy);
        clampToCropArea();
        applyImageMatrix();
    }

    private void applyImageMatrix() {
        setImageMatrix(imageMatrixInternal);
        invalidate();
    }

    private void clampToCropArea() {
        if (sourceBitmap == null) {
            return;
        }

        bitmapRect.set(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        mappedRect.set(bitmapRect);
        imageMatrixInternal.mapRect(mappedRect);

        float diameter = getCropDiameter();
        float cropLeft = (getWidth() - diameter) / 2f;
        float cropTop = (getHeight() - diameter) / 2f;
        float cropRight = cropLeft + diameter;
        float cropBottom = cropTop + diameter;

        float dx = 0f;
        float dy = 0f;

        if (mappedRect.left > cropLeft) {
            dx = cropLeft - mappedRect.left;
        } else if (mappedRect.right < cropRight) {
            dx = cropRight - mappedRect.right;
        }

        if (mappedRect.top > cropTop) {
            dy = cropTop - mappedRect.top;
        } else if (mappedRect.bottom < cropBottom) {
            dy = cropBottom - mappedRect.bottom;
        }

        imageMatrixInternal.postTranslate(dx, dy);
    }

    private float getCropDiameter() {
        float size = Math.min(getWidth(), getHeight()) - (overlayPaddingPx * 2f);
        if (size <= 0f) {
            size = Math.min(getWidth(), getHeight());
        }
        return Math.max(1f, size);
    }

    private float getCurrentScale() {
        imageMatrixInternal.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (sourceBitmap == null) {
                return false;
            }
            float currentScale = getCurrentScale();
            float targetScale = currentScale * detector.getScaleFactor();
            float factor = detector.getScaleFactor();

            if (targetScale < minScale) {
                factor = minScale / currentScale;
            } else if (targetScale > maxScale) {
                factor = maxScale / currentScale;
            }

            imageMatrixInternal.postScale(
                    factor,
                    factor,
                    detector.getFocusX(),
                    detector.getFocusY()
            );
            clampToCropArea();
            applyImageMatrix();
            return true;
        }
    }
}
