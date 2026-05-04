package ru.student.mymeteo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class RiskTrendView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint softLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = new int[]{20, 35, 40, 25};
    private String[] labels = new String[]{"утро", "сейч.", "завтр.", "+2"};
    private String[] notes = new String[]{"", "текущий индекс", "", ""};
    private OnPointClickListener pointClickListener;
    private int lastLeft;
    private int lastRight;
    private int lastTop;
    private int lastBottom;

    public interface OnPointClickListener {
        void onPointClick(int index);
    }

    public RiskTrendView(Context context) {
        super(context);
        linePaint.setColor(Color.parseColor("#212842"));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        softLinePaint.setColor(Color.parseColor("#8FA0C7"));
        softLinePaint.setStrokeWidth(16f);
        softLinePaint.setStyle(Paint.Style.STROKE);
        softLinePaint.setStrokeCap(Paint.Cap.ROUND);
        softLinePaint.setStrokeJoin(Paint.Join.ROUND);
        softLinePaint.setAlpha(70);
        fillPaint.setColor(Color.parseColor("#D9A441"));
        fillPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.parseColor("#687086"));
        textPaint.setTextSize(24f);
        textPaint.setFakeBoldText(true);
        axisPaint.setColor(Color.parseColor("#D9CFBC"));
        axisPaint.setStrokeWidth(2f);
        axisPaint.setStyle(Paint.Style.STROKE);
        notePaint.setColor(Color.parseColor("#212842"));
        notePaint.setTextSize(21f);
    }

    public void setValues(int[] values) {
        if (values != null && values.length >= 2) {
            this.values = values.clone();
            invalidate();
        }
    }

    public void setTimeline(String[] labels, String[] notes) {
        if (labels != null && labels.length == values.length) {
            this.labels = labels.clone();
        }
        if (notes != null && notes.length == values.length) {
            this.notes = notes.clone();
        }
        invalidate();
    }

    public void setOnPointClickListener(OnPointClickListener listener) {
        this.pointClickListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && pointClickListener != null && values.length > 1) {
            float step = (lastRight - lastLeft) / (float) (values.length - 1);
            int nearest = 0;
            float bestDistance = Float.MAX_VALUE;
            for (int index = 0; index < values.length; index++) {
                float x = lastLeft + step * index;
                float distance = Math.abs(event.getX() - x);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = index;
                }
            }
            pointClickListener.onPointClick(nearest);
            return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int left = 56;
        int right = width - 18;
        int top = 22;
        int bottom = height - 42;
        lastLeft = left;
        lastRight = right;
        lastTop = top;
        lastBottom = bottom;
        float step = (right - left) / (float) (values.length - 1);

        drawAxes(canvas, left, right, top, bottom);

        Path line = new Path();
        line.moveTo(left, yFor(values[0], top, bottom));
        for (int index = 1; index < values.length; index++) {
            float prevX = left + step * (index - 1);
            float prevY = yFor(values[index - 1], top, bottom);
            float x = left + step * index;
            float y = yFor(values[index], top, bottom);
            float midX = (prevX + x) / 2f;
            line.cubicTo(midX, prevY, midX, y, x, y);
        }
        canvas.drawPath(line, softLinePaint);
        canvas.drawPath(line, linePaint);

        for (int index = 0; index < values.length; index++) {
            float x = left + step * index;
            float y = yFor(values[index], top, bottom);
            fillPaint.setColor(colorFor(values[index]));
            canvas.drawCircle(x, y, 13f, fillPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[index], x, height - 10, textPaint);
            if (notes[index] != null && !notes[index].isEmpty() && values[index] >= 30) {
                notePaint.setTextAlign(index == values.length - 1 ? Paint.Align.RIGHT : Paint.Align.LEFT);
                float noteX = index == values.length - 1 ? x : x + 10f;
                float noteY = Math.max(top + 18f, y - 15f);
                canvas.drawText(notes[index], noteX, noteY, notePaint);
            }
        }
    }

    private void drawAxes(Canvas canvas, int left, int right, int top, int bottom) {
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        drawRiskLine(canvas, "выс.", 58, left, right, top, bottom);
        drawRiskLine(canvas, "ср.", 30, left, right, top, bottom);
        drawRiskLine(canvas, "низ.", 8, left, right, top, bottom);
    }

    private void drawRiskLine(Canvas canvas, String label, int value, int left, int right, int top, int bottom) {
        float y = yFor(value, top, bottom);
        axisPaint.setAlpha(value == 8 ? 0 : 90);
        canvas.drawLine(left, y, right, y, axisPaint);
        axisPaint.setAlpha(255);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(label, left - 6, y + 8, textPaint);
    }

    private float yFor(int value, int top, int bottom) {
        float normalized = Math.max(0, Math.min(100, value)) / 100f;
        return bottom - (bottom - top) * normalized;
    }

    private int colorFor(int value) {
        if (value >= 58) {
            return Color.parseColor("#C85C5C");
        }
        if (value >= 30) {
            return Color.parseColor("#D9A441");
        }
        return Color.parseColor("#6FAF7A");
    }
}
