package com.example.aquatech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.PathParser;

public class WaterDropView extends View {

    private Paint dropPaint, wavePaint, textPaint;
    private Path dropPath;
    private RectF dropBounds;
    private float wavePhase = 0f;
    private float waveStrength = 12f;
    private float waveSpeed = 2.0f;
    private int waveProgress = 0;

    public WaterDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dropPaint.setStyle(Paint.Style.FILL);
        dropPaint.setColor(Color.argb(80, 54, 171, 225));

        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(75f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        try {
            Typeface poppins = ResourcesCompat.getFont(context, R.font.poppins_black);
            textPaint.setTypeface(poppins);
        } catch (Exception e) {
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        dropPath = new Path();
        dropBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        String pathData = "M363,44.429 C356.657,45.3072 351.354,46.9115 346,50.5201 " +
                "C341.772,53.37 338.173,56.7046 335.387,61 C318.282,87.3804 " +
                "342.005,120.455 372,116.816 C376.525,116.267 381.073,114.38 385,112.12 " +
                "C417.306,93.5327 400.97,39.1725 363,44.429 M260,73.8187 " +
                "C254.177,75.8619 248.967,80.4648 244,84 C232.916,91.8897 " +
                "222.432,100.36 212,109.08 C156.705,155.305 112.993,224.571 118.09,299 " +
                "C122.527,363.805 176.109,417.467 241,421.91 C260.015,423.213 " +
                "279.134,419.672 297,413.308 C368.141,387.966 402.511,300.181 370.216,233 " +
                "C356.103,203.641 329.298,186.475 308.171,163 C294.844,148.193 " +
                "284.086,130.272 278.745,111 C276.109,101.486 275.912,91.5121 273.61,82 " +
                "C272.092,75.7289 266.373,71.5824 260,73.8187 Z";

        dropPath = PathParser.createPathFromPathData(pathData);
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(w / 500f, h / 500f);
        dropPath.transform(scaleMatrix);
        dropPath.computeBounds(dropBounds, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(dropPath, dropPaint);

        String progressText = waveProgress + "%";
        float textX = getWidth() / 2f;
        float textY = getHeight() / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f);

        textPaint.setColor(Color.parseColor("#36ABE1"));
        canvas.drawText(progressText, textX, textY, textPaint);

        canvas.save();
        canvas.clipPath(dropPath);

        float waveBase = dropBounds.bottom - (dropBounds.height() * (waveProgress / 100f));
        Shader waveGradient = new LinearGradient(0, waveBase, 0, dropBounds.bottom,
                new int[]{Color.parseColor("#36ABE1"), Color.parseColor("#3775BB")},
                null, Shader.TileMode.CLAMP);
        wavePaint.setShader(waveGradient);

        Path wavePath = new Path();
        wavePath.moveTo(dropBounds.left, waveBase);
        for (float x = dropBounds.left; x <= dropBounds.right; x += 2f) {
            double angle = ((x - dropBounds.left) + wavePhase) * Math.PI / 180.0;
            float y = (float) (Math.sin(angle) * waveStrength) + waveBase;
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(dropBounds.right, dropBounds.bottom);
        wavePath.lineTo(dropBounds.left, dropBounds.bottom);
        wavePath.close();

        canvas.drawPath(wavePath, wavePaint);
        canvas.restore();

        canvas.save();
        canvas.clipPath(dropPath);
        RectF waterArea = new RectF(0, waveBase, getWidth(), getHeight());
        canvas.clipRect(waterArea);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(progressText, textX, textY, textPaint);
        canvas.restore();

        wavePhase += waveSpeed;
        if (wavePhase > 360f) wavePhase = 0f;
        postInvalidateOnAnimation();
    }

    public void setWaveProgress(int progress) {
        this.waveProgress = Math.max(0, Math.min(progress, 100));
        invalidate();
    }

    public int getWaveProgress() {
        return waveProgress;
    }
}
