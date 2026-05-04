package ru.student.mymeteo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class FactorIconView extends View {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint softFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String tag;
    private int accentColor;

    public FactorIconView(Context context, String tag, int accentColor) {
        super(context);
        this.tag = tag;
        this.accentColor = accentColor;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(9f);
        fill.setStyle(Paint.Style.FILL);
        softFill.setStyle(Paint.Style.FILL);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
    }

    public void setIcon(String tag, int accentColor) {
        this.tag = tag;
        this.accentColor = accentColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        stroke.setColor(accentColor);
        fill.setColor(accentColor);
        softFill.setColor(withAlpha(accentColor, 42));

        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float size = Math.min(width, height);
        canvas.drawCircle(cx, cy, size * 0.47f, softFill);

        if ("pressure".equals(tag)) {
            drawPressure(canvas, width, height);
        } else if ("temperature".equals(tag)) {
            drawThermometer(canvas, cx, cy, size);
        } else if ("humidity".equals(tag)) {
            drawDrop(canvas, cx, cy, size);
        } else if ("geomagnetic".equals(tag)) {
            drawMagnetic(canvas, cx, cy, size);
        } else if ("diary".equals(tag)) {
            drawDiary(canvas, width, height);
        } else {
            canvas.drawCircle(cx, cy, size * 0.28f, fill);
        }
    }

    private void drawPressure(Canvas canvas, float width, float height) {
        Path wave = new Path();
        wave.moveTo(width * 0.12f, height * 0.52f);
        wave.cubicTo(width * 0.28f, height * 0.25f, width * 0.38f, height * 0.78f, width * 0.52f, height * 0.52f);
        wave.cubicTo(width * 0.66f, height * 0.25f, width * 0.76f, height * 0.78f, width * 0.90f, height * 0.52f);
        canvas.drawPath(wave, stroke);
        canvas.drawCircle(width * 0.5f, height * 0.5f, Math.min(width, height) * 0.38f, stroke);
    }

    private void drawThermometer(Canvas canvas, float cx, float cy, float size) {
        float bulb = size * 0.18f;
        RectF tube = new RectF(cx - size * 0.08f, cy - size * 0.35f, cx + size * 0.08f, cy + size * 0.16f);
        canvas.drawRoundRect(tube, size * 0.08f, size * 0.08f, stroke);
        canvas.drawCircle(cx, cy + size * 0.24f, bulb, fill);
        canvas.drawLine(cx, cy - size * 0.20f, cx, cy + size * 0.20f, stroke);
    }

    private void drawDrop(Canvas canvas, float cx, float cy, float size) {
        Path drop = new Path();
        drop.moveTo(cx, cy - size * 0.38f);
        drop.cubicTo(cx + size * 0.30f, cy - size * 0.05f, cx + size * 0.34f, cy + size * 0.18f, cx, cy + size * 0.34f);
        drop.cubicTo(cx - size * 0.34f, cy + size * 0.18f, cx - size * 0.30f, cy - size * 0.05f, cx, cy - size * 0.38f);
        canvas.drawPath(drop, fill);
    }

    private void drawMagnetic(Canvas canvas, float cx, float cy, float size) {
        Paint oldStroke = new Paint(stroke);
        stroke.setStrokeWidth(8f);

        RectF fieldOuter = new RectF(cx - size * 0.44f, cy - size * 0.40f, cx + size * 0.44f, cy + size * 0.40f);
        RectF fieldInner = new RectF(cx - size * 0.32f, cy - size * 0.28f, cx + size * 0.32f, cy + size * 0.28f);
        canvas.drawArc(fieldOuter, 205, 130, false, stroke);
        canvas.drawArc(fieldInner, 205, 130, false, stroke);

        stroke.setStrokeWidth(12f);
        RectF magnet = new RectF(cx - size * 0.30f, cy - size * 0.18f, cx + size * 0.30f, cy + size * 0.46f);
        canvas.drawArc(magnet, 0, 180, false, stroke);
        canvas.drawLine(cx - size * 0.30f, cy + size * 0.14f, cx - size * 0.30f, cy + size * 0.42f, stroke);
        canvas.drawLine(cx + size * 0.30f, cy + size * 0.14f, cx + size * 0.30f, cy + size * 0.42f, stroke);

        RectF leftPole = new RectF(cx - size * 0.42f, cy + size * 0.32f, cx - size * 0.18f, cy + size * 0.54f);
        RectF rightPole = new RectF(cx + size * 0.18f, cy + size * 0.32f, cx + size * 0.42f, cy + size * 0.54f);
        canvas.drawRoundRect(leftPole, 7f, 7f, fill);
        canvas.drawRoundRect(rightPole, 7f, 7f, fill);

        labelPaint.setTextSize(size * 0.18f);
        canvas.drawText("N", leftPole.centerX(), leftPole.centerY() + size * 0.06f, labelPaint);
        canvas.drawText("S", rightPole.centerX(), rightPole.centerY() + size * 0.06f, labelPaint);
        stroke.set(oldStroke);
    }

    private void drawDiary(Canvas canvas, float width, float height) {
        RectF note = new RectF(width * 0.22f, height * 0.16f, width * 0.78f, height * 0.84f);
        canvas.drawRoundRect(note, 8f, 8f, stroke);
        canvas.drawLine(width * 0.34f, height * 0.34f, width * 0.66f, height * 0.34f, stroke);
        canvas.drawLine(width * 0.34f, height * 0.48f, width * 0.62f, height * 0.48f, stroke);
        Path heart = new Path();
        heart.moveTo(width * 0.50f, height * 0.72f);
        heart.cubicTo(width * 0.30f, height * 0.60f, width * 0.38f, height * 0.46f, width * 0.50f, height * 0.56f);
        heart.cubicTo(width * 0.62f, height * 0.46f, width * 0.70f, height * 0.60f, width * 0.50f, height * 0.72f);
        canvas.drawPath(heart, fill);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
