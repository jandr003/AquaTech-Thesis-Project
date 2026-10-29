package com.example.aquatech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AquaTechCircleGaugeView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;

    private float percentage = 0f;
    private final RectF arcBounds = new RectF();

    public AquaTechCircleGaugeView(Context context) {
        super(context);
        init();
    }

    public AquaTechCircleGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AquaTechCircleGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(Color.parseColor("#E2E8F0"));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(16f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#10B981"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(16f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#0F172A"));
        textPaint.setTextSize(44f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        percentage = 75f;
    }

    public void setPercentage(float pct) {
        this.percentage = Math.max(0f, Math.min(100f, pct));
        invalidate();
    }

    public float getPercentage() {
        return percentage;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        float strokeWidth = 16f;
        float padding = strokeWidth / 2f + 8f;

        arcBounds.set(padding, padding, width - padding, height - padding);

        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint);

        float sweepAngle = (percentage / 100f) * 360f;
        if (sweepAngle > 0) {
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint);
        }

        int pctInt = Math.round(percentage);
        canvas.drawText(pctInt + "%", width / 2f, height / 2f + 14f, textPaint);
    }
}
