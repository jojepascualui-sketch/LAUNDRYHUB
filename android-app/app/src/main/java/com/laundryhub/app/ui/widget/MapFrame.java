package com.laundryhub.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.laundryhub.app.R;

/**
 * The 250 dp map frame of 04a: 22 dp rounded corners and a 2 dp #D5DBD6 border drawn on top of
 * its children. A Google map cannot be clipped to rounded corners, so the corners are masked in
 * the screen background color instead. Touches on the map do not scroll the page.
 */
// OOP: Inheritance — a FrameLayout that draws a rounded frame over its children.
public class MapFrame extends FrameLayout {

    // OOP: Encapsulation — paints and geometry are private and rebuilt on size changes.
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mask = new Path();
    private final RectF inner = new RectF();
    private final float radius;

    /**
     * @param context context
     */
    public MapFrame(@NonNull Context context) {
        this(context, null);
    }

    /**
     * @param context context
     * @param attrs   XML attributes
     */
    public MapFrame(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        radius = getResources().getDimension(R.dimen.map_radius);
        float border = getResources().getDimension(R.dimen.map_border);
        maskPaint.setColor(ContextCompat.getColor(context, R.color.bg));
        maskPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.icon_button_border));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(border);
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float half = borderPaint.getStrokeWidth() / 2f;
        inner.set(half, half, w - half, h - half);
        mask.reset();
        mask.setFillType(Path.FillType.EVEN_ODD);
        mask.addRect(0, 0, w, h, Path.Direction.CW);
        mask.addRoundRect(inner, radius, radius, Path.Direction.CW);
    }

    /** Children (the map, chip, button) first, then the corner mask and border on top. */
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawPath(mask, maskPaint);
        canvas.drawRoundRect(inner, radius, radius, borderPaint);
    }

    /** Keeps the scrolling step from stealing pan and drag gestures from the map. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (getParent() != null && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.dispatchTouchEvent(event);
    }
}
