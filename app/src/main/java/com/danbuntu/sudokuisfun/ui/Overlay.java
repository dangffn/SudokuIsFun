package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.danbuntu.sudokuisfun.R;

/**
 * Created by Dan on 4/27/2016.
 */
public class Overlay extends View implements View.OnTouchListener {

    Rect visibleArea, leftArea, topArea, rightArea, bottomArea;
    int w, h;
    Paint marginPaint, borderPaint, draggablePaint, linePaint;
    boolean resizable = false;
    Rect topRightO, bottomLeftO, topLeftO, bottomRightO, draggingRect, selectionBounds;
    int radius;
    int PAN = 1;
    int RESIZE = 2;
    int MODE = -1;
    Point lastTouch;
    float dp2, dp3, dp4, dp5;

    public Overlay(Context context) {
        super(context);
        init();
    }

    public Overlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Overlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public Rect getVisibleArea() {
        return visibleArea;
    }

    public void setResizable() {


        if (visibleArea != null) {

            topRightO = new Rect();
            bottomLeftO = new Rect();
            topLeftO = new Rect();
            bottomRightO = new Rect();

            resizable = true;

            lastTouch = new Point();

            setOnTouchListener(this);
        }

    }

    private void init() {
        dp2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        dp3 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        dp4 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        dp5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);

        marginPaint = new Paint();
        marginPaint.setStyle(Paint.Style.FILL);
        marginPaint.setColor(getResources().getColor(R.color.overlay_background));

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(getResources().getColor(R.color.grid_line_thick));
        borderPaint.setStrokeWidth(dp5);

        draggablePaint = new Paint();
        draggablePaint.setStrokeWidth(dp2);

        visibleArea = new Rect();
        topArea = new Rect();
        bottomArea = new Rect();
        leftArea = new Rect();
        rightArea = new Rect();
    }

    public static Rect getBitmapRect(ImageView imageView, Bitmap bitmap) {
        Rect rect = new Rect();

        if (imageView.getDrawable() != null) {


            float bitmapWScale = (float) bitmap.getWidth() / imageView.getWidth();
            float bitmapHScale = (float) bitmap.getHeight() / imageView.getHeight();

            float scale = Math.max(bitmapWScale, bitmapHScale);

            int bitmapWidth = (int) (bitmap.getWidth() / scale);
            int bitmapHeight = (int) (bitmap.getHeight() / scale);

            int bitmapLeft = (imageView.getWidth() / 2) - (bitmapWidth / 2);
            int bitmapTop = (imageView.getHeight() / 2) - (bitmapHeight / 2);

            rect.set(bitmapLeft, bitmapTop, bitmapLeft + bitmapWidth, bitmapTop + bitmapHeight);


            Log.i("Bitmap Bounds", "left=" + rect.left + " top=" + rect.top + " right=" + rect.right + " bottom=" + rect.bottom);


        } else {
            return null;
        }
        return rect;
    }

    public Rect getSelectionBounds() {
        return selectionBounds;
    }

    private void drawGridLines(Canvas canvas, Rect outerRect, Paint linePaint, int color, int width) {
        float hThird = outerRect.height() / 3f;
        float wThird = outerRect.width() / 3f;

        linePaint.setColor(color);
        linePaint.setStrokeWidth(width);

        for(int y=1; y<3; y++) {
            float top = outerRect.top + (y * hThird);
            canvas.drawLine(outerRect.left,
                    top,
                    outerRect.right,
                    top,
                    linePaint);
        }

        for(int x=1; x<3; x++) {
            float left = outerRect.left + (x * wThird);
            canvas.drawLine(left,
                    outerRect.top,
                    left,
                    outerRect.bottom,
                    linePaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;

        int hw = (w / 2);
        int hh = (h / 2);

        int ra = (int)((Math.min(h, w) * 0.8f) / 2);

        int l = hw - ra;
        int r = hw + ra;
        int t = hh - ra;
        int b = hh + ra;

        visibleArea.set(l, t, r, b);
        topArea.set(0, 0, w, t);
        leftArea.set(0, 0, l, h);
        rightArea.set(r, 0, w, h);
        bottomArea.set(0, b, w, h);

        // the radius of the resize draggables
        radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());

        if(resizable) positionDraggables();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(visibleArea != null) {

            canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), 200,
                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
            canvas.drawRect(topArea, marginPaint);
            canvas.drawRect(leftArea, marginPaint);
            canvas.drawRect(rightArea, marginPaint);
            canvas.drawRect(bottomArea, marginPaint);
            canvas.restore();

            drawGridLines(canvas, visibleArea, linePaint, 0xFF888888, (int)dp3);
            canvas.drawRect(visibleArea, borderPaint);

            if (resizable) {

                draggablePaint.setStyle(Paint.Style.FILL);
                draggablePaint.setColor(getResources().getColor(R.color.colorAccent));

                canvas.drawCircle(topLeftO.centerX(), topLeftO.centerY(), radius, draggablePaint);
                canvas.drawCircle(bottomRightO.centerX(), bottomRightO.centerY(), radius, draggablePaint);
                canvas.drawCircle(topRightO.centerX(), topRightO.centerY(), radius, draggablePaint);
                canvas.drawCircle(bottomLeftO.centerX(), bottomLeftO.centerY(), radius, draggablePaint);

                draggablePaint.setStyle(Paint.Style.STROKE);
                draggablePaint.setColor(Color.BLACK);

                canvas.drawCircle(topLeftO.centerX(), topLeftO.centerY(), radius, draggablePaint);
                canvas.drawCircle(bottomRightO.centerX(), bottomRightO.centerY(), radius, draggablePaint);
                canvas.drawCircle(topRightO.centerX(), topRightO.centerY(), radius, draggablePaint);
                canvas.drawCircle(bottomLeftO.centerX(), bottomLeftO.centerY(), radius, draggablePaint);

            }
        }

        super.onDraw(canvas);
    }

    public void resizeArea(MotionEvent event) {
        if (draggingRect == null) return;

        int l, t, r, b;

        if (selectionBounds == null) {
            l = 0;
            t = 0;
            r = w;
            b = h;
        } else {
            l = selectionBounds.left;
            t = selectionBounds.top;
            r = selectionBounds.right;
            b = selectionBounds.bottom;
        }

        if(draggingRect == topLeftO || draggingRect == bottomLeftO)
            visibleArea.left = Math.max(l, Math.min(visibleArea.right - 50, lastTouch.x));
        if(draggingRect == topLeftO || draggingRect == topRightO)
            visibleArea.top = Math.max(t, Math.min(visibleArea.bottom - 50, lastTouch.y));
        if(draggingRect == topRightO || draggingRect == bottomRightO)
            visibleArea.right = Math.min(r, Math.max(visibleArea.left + 50, lastTouch.x));
        if(draggingRect == bottomRightO || draggingRect == bottomLeftO)
            visibleArea.bottom = Math.min(b, Math.max(visibleArea.top + 50, lastTouch.y));

        leftArea.right = visibleArea.left;
        rightArea.left = visibleArea.right;
        topArea.bottom  = visibleArea.top;
        bottomArea.top = visibleArea.bottom;

        positionDraggables();
        invalidate();

    }

    private void positionDraggables() {
        topLeftO.set(visibleArea.left - radius, visibleArea.top - radius, visibleArea.left + radius, visibleArea.top + radius);
        bottomRightO.set(visibleArea.right - radius, visibleArea.bottom - radius, visibleArea.right + radius, visibleArea.bottom + radius);
        topRightO.set(visibleArea.right - radius, visibleArea.top - radius, visibleArea.right + radius, visibleArea.top + radius);
        bottomLeftO.set(visibleArea.left - radius, visibleArea.bottom - radius, visibleArea.left + radius, visibleArea.bottom + radius);
    }

    private void pan(MotionEvent event) {

        float xShift = event.getX() - lastTouch.x;
        float yShift = event.getY() - lastTouch.y;

        if (selectionBounds != null) {
            if (visibleArea.left + xShift < selectionBounds.left)
                xShift = selectionBounds.left - visibleArea.left;
            if (visibleArea.right + xShift > selectionBounds.right)
                xShift = selectionBounds.right - visibleArea.right;
            if (visibleArea.top + yShift < selectionBounds.top)
                yShift = selectionBounds.top - visibleArea.top;
            if (visibleArea.bottom + yShift > selectionBounds.bottom)
                yShift = selectionBounds.bottom - visibleArea.bottom;
        } else {
            if (visibleArea.left + xShift < 0) xShift = -visibleArea.left;
            if (visibleArea.right + xShift > w) xShift = w - visibleArea.right;
            if (visibleArea.top + yShift < 0) yShift = -visibleArea.top;
            if (visibleArea.bottom + yShift > h) yShift = h - visibleArea.bottom;
        }

        visibleArea.top += yShift;
        visibleArea.bottom += yShift;
        visibleArea.left += xShift;
        visibleArea.right += xShift;

        leftArea.right += xShift;
        rightArea.left += xShift;
        topArea.bottom += yShift;
        bottomArea.top += yShift;

        positionDraggables();

        invalidate();

    }

    public void setSelectionBounds(Rect rectBounds) {

        this.selectionBounds = rectBounds;

        if (visibleArea.top < selectionBounds.top || visibleArea.left < selectionBounds.left ||
                visibleArea.right > selectionBounds.right || visibleArea.bottom > selectionBounds.bottom) {

            int smallestRadius = Math.min(selectionBounds.width(), selectionBounds.height()) / 2;

            visibleArea.set(selectionBounds.centerX() - smallestRadius, selectionBounds.centerY() - smallestRadius,
                    selectionBounds.centerX() + smallestRadius, selectionBounds.centerY() + smallestRadius);
            rightArea.left = visibleArea.right;
            leftArea.right = visibleArea.left;
            topArea.bottom = visibleArea.top;
            bottomArea.top = visibleArea.bottom;

        }

        positionDraggables();

        invalidate();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!resizable) return false;

        switch (event.getActionMasked()) {

            case (MotionEvent.ACTION_DOWN):
                lastTouch.set((int) event.getX(), (int) event.getY());

                if (topLeftO.contains((int) event.getX(), (int) event.getY())) {
                    draggingRect = topLeftO;
                } else if (bottomRightO.contains((int) event.getX(), (int) event.getY())) {
                    draggingRect = bottomRightO;
                } else if (topRightO.contains((int) event.getX(), (int) event.getY())) {
                    draggingRect = topRightO;
                } else if (bottomLeftO.contains((int) event.getX(), (int) event.getY())) {
                    draggingRect = bottomLeftO;
                } else if (visibleArea.contains((int) event.getX(), (int) event.getY())) {
                    MODE = PAN;
                    break;
                }
                MODE = RESIZE;
                break;

            case (MotionEvent.ACTION_MOVE):
                if (MODE == RESIZE) {
                    resizeArea(event);
                } else if (MODE == PAN) {
                    pan(event);
                }
                lastTouch.set((int) event.getX(), (int) event.getY());
                break;

            case (MotionEvent.ACTION_UP):
                draggingRect = null;
                MODE = -1;
                break;

        }


        return true;
    }
}
