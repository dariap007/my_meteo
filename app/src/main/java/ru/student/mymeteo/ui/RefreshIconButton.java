package ru.student.mymeteo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class RefreshIconButton extends View {
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RefreshIconButton(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        circlePaint.setColor(Color.parseColor("#212842"));
        circlePaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
        arrowPaint.setStrokeWidth(3.8f);
        tipPaint.setColor(Color.WHITE);
        tipPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = size * 0.36f;
        canvas.drawCircle(cx, cy, radius, circlePaint);

        RectF arc = new RectF(cx - size * 0.18f, cy - size * 0.18f, cx + size * 0.18f, cy + size * 0.18f);
        canvas.drawArc(arc, 35, 285, false, arrowPaint);

        float tipX = cx + size * 0.155f;
        float tipY = cy - size * 0.115f;
        android.graphics.Path tip = new android.graphics.Path();
        tip.moveTo(tipX, tipY);
        tip.lineTo(tipX - size * 0.015f, tipY - size * 0.095f);
        tip.lineTo(tipX + size * 0.085f, tipY - size * 0.045f);
        tip.close();
        canvas.drawPath(tip, tipPaint);
    }
}
