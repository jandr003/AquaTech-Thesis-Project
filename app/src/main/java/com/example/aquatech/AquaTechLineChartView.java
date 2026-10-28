package com.example.aquatech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AquaTechLineChartView extends View {

    private Paint gridPaint;
    private Paint linePaint;
    private Paint areaPaint;
    private Paint outerDotPaint;
    private Paint innerDotPaint;
    private Paint textPaint;

    private List<String> monthLabels = new ArrayList<>();
    private List<Float> dataValues = new ArrayList<>();

    public AquaTechLineChartView(Context context) {
        super(context);
        init();
    }

    public AquaTechLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AquaTechLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F1F5F9"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#2196F3"));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        areaPaint.setColor(Color.parseColor("#152196F3"));
        areaPaint.setStyle(Paint.Style.FILL);

        outerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerDotPaint.setColor(Color.parseColor("#2196F3"));
        outerDotPaint.setStyle(Paint.Style.FILL);

        innerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerDotPaint.setColor(Color.WHITE);
        innerDotPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        loadSampleData();
    }

    private void loadSampleData() {
        monthLabels.clear();
        dataValues.clear();

        Calendar cal = Calendar.getInstance();
        int currentMonthIndex = cal.get(Calendar.MONTH);

        String[] allMonths = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        float[] sampleValues = {32f, 44f, 18f, 28f, 53f, 78f, 65f, 82f, 59f, 71f, 45f, 90f};

        for (int i = 11; i >= 0; i--) {
            int mIndex = (currentMonthIndex - i + 12) % 12;
            monthLabels.add(allMonths[mIndex]);
            dataValues.add(sampleValues[11 - i]);
        }
    }

    public void setChartData(List<String> labels, List<Float> values) {
        if (labels != null && values != null && labels.size() == values.size() && !labels.isEmpty()) {
            this.monthLabels = new ArrayList<>(labels);
            this.dataValues = new ArrayList<>(values);
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = 360;
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : desiredHeight;

        int numMonths = (dataValues != null && !dataValues.isEmpty()) ? dataValues.size() : 5;
        int calculatedWidth;
        if (numMonths > 5 && parentWidth > 0) {
            calculatedWidth = Math.round((parentWidth / 5.0f) * numMonths);
        } else {
            calculatedWidth = Math.max(parentWidth, 600);
        }

        setMeasuredDimension(calculatedWidth, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        if (dataValues == null || dataValues.isEmpty()) {
            loadSampleData();
        }

        float paddingLeft = 70f;
        float paddingRight = 40f;
        float paddingTop = 30f;
        float paddingBottom = 75f;

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        if (chartWidth <= 0 || chartHeight <= 0) return;

        float maxVal = 10f;
        for (float val : dataValues) {
            if (val > maxVal) maxVal = val;
        }
        maxVal = (float) Math.ceil(maxVal / 10.0) * 10f;

        int numGridLines = 4;
        for (int i = 0; i <= numGridLines; i++) {
            float y = paddingTop + (chartHeight / numGridLines) * i;
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);

            int labelVal = Math.round(maxVal - (maxVal / numGridLines) * i);
            canvas.drawText(String.valueOf(labelVal), paddingLeft - 28f, y + 4f, textPaint);
        }

        int size = dataValues.size();
        float stepX = size > 1 ? chartWidth / (size - 1) : chartWidth;

        float[] pointX = new float[size];
        float[] pointY = new float[size];

        for (int i = 0; i < size; i++) {
            pointX[i] = paddingLeft + (i * stepX);
            float ratio = dataValues.get(i) / maxVal;
            pointY[i] = paddingTop + chartHeight * (1f - ratio);
        }

        Path linePath = new Path();
        Path areaPath = new Path();

        linePath.moveTo(pointX[0], pointY[0]);
        areaPath.moveTo(pointX[0], paddingTop + chartHeight);
        areaPath.lineTo(pointX[0], pointY[0]);

        for (int i = 1; i < size; i++) {
            linePath.lineTo(pointX[i], pointY[i]);
            areaPath.lineTo(pointX[i], pointY[i]);
        }

        areaPath.lineTo(pointX[size - 1], paddingTop + chartHeight);
        areaPath.close();

        canvas.drawPath(areaPath, areaPaint);
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < size; i++) {
            canvas.drawCircle(pointX[i], pointY[i], 12f, outerDotPaint);
            canvas.drawCircle(pointX[i], pointY[i], 6f, innerDotPaint);
            canvas.drawText(monthLabels.get(i), pointX[i], height - 16f, textPaint);
        }
    }
}
