package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.danbuntu.sudokuisfun.R;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Dan on 5/28/2016. Have a great day!
 */
public class RainingNumbers extends View implements View.OnTouchListener {

    private final int MAX_NUMBERS = 50;
    private final long touchDelay = 100;

    private Random rng;
    private Queue<FallingDigit> queue;
    private boolean running;
    private Paint textPaint, backgroundPaint;
    private Thread addNumbers, update;
    private int dp40, dp20;
    private int cX, cY, radius;
    private int gravity, bgColor;
    private Context context;
    private long lastTouchDown;
    private String[] chars;
    private int[] colors;
    private final static int GRAV_DELAY = 40;
    private boolean layedOut = false;

    public RainingNumbers(Context context) {
        this(context, null);
    }

    public RainingNumbers(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RainingNumbers(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    public void setChars(String[] chars) {
        if(chars != null && chars.length != 9) {
            // its ok if chars is null, but if its an array that's not 9 items long, it may cause IndexOutOfBoundsException
            this.chars = null;
        } else {
            this.chars = chars;
        }
    }

    public void setColors(int[] colors) {
        if(colors != null && colors.length != 9) {
            // its ok if colors is null, but if its an array that's not 9 items long, it may cause IndexOutOfBoundsException
            this.colors = null;
        } else {
            this.colors = colors;
        }
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setDither(true);

        bgColor = context.getResources().getColor(R.color.palette09);

        queue = new ConcurrentLinkedQueue<>();

        dp40 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        dp20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf"));
        textPaint.setTextSize(dp40);
        textPaint.setAntiAlias(true);

        rng = new Random();

        gravity = Math.max(1, (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()) * 0.85));

        setOnTouchListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        cX = w / 2;
        cY = h / 2;
        radius = Math.max(w, h) / 2;

        backgroundPaint.setShader(new RadialGradient(cX,
                cY,
                radius,
                getResources().getColor(R.color.palette05),
                getResources().getColor(R.color.palette09),
                Shader.TileMode.CLAMP));

        layedOut = true;

    }

    public void start() {
        if (isRunning()) {
            Log.e("RainingNumbers", "start() called while already running");
            return;
        }

        if(!layedOut) {
            Log.i("RainingNumbers", "start() called but view is not layed out, adding layout listener");
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    start();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
            return;
        }

        running = true;

        Log.i("RainingNumbers", "Creating threads");
        addNumbers = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning()) {
                    if (queue.size() < MAX_NUMBERS) {
                        queue.add(new FallingDigit());
                    }
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        Log.e("Thread", "Interrupted");
                    }
                }
            }
        });

        update = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning()) {

                    applyGravity();
                    RainingNumbers.this.postInvalidate();

                    try {
                        Thread.sleep(GRAV_DELAY);
                    } catch(InterruptedException ignored) {}
                }
            }
        });

        addNumbers.start();
        update.start();
    }

    public void stop() {
        if(!isRunning()) {
            Log.e("RainingNumbers", "stop() called while already stopped");
            return;
        }

        running = false;

        if(addNumbers != null) {
            Log.i("RainingNumbers", "Destroying addNumbers thread");
            addNumbers.interrupt();
            addNumbers = null;
        }

        if(update != null) {
            Log.i("RainingNumbers", "Destroying update thread");
            update.interrupt();
            update = null;
        }
    }

    public void applyGravity() {
        for (FallingDigit drop : queue) {
            if (drop.transparency >= 150 && drop.colorShift > 0) {
                drop.colorShift = -drop.colorShift;
            }
            drop.transparency += drop.colorShift;
            drop.y += gravity;
        }
    }

    private void drawDigit(Canvas canvas, FallingDigit digit) {

        if(digit.transparency <= 0 || digit.y > (getBottom() + digit.size)) {
            queue.remove(digit);
            return;
        }

        if(colors != null) {
            textPaint.setColor((digit.transparency << 24 | 0xFFFFFF) & colors[digit.value]);
        } else {
            textPaint.setColor(digit.transparency << 24);
        }
        canvas.drawText(digit.character, digit.x, digit.y, textPaint);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(bgColor);

        if(cX > 0 && cY > 0) {
            canvas.drawCircle(cX,
                    cY,
                    radius,
                    backgroundPaint);

            for (FallingDigit drop : queue) {
                drawDigit(canvas, drop);
            }
        }

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getActionMasked()) {

            case(MotionEvent.ACTION_DOWN):
            case(MotionEvent.ACTION_MOVE):
                if(System.currentTimeMillis() - lastTouchDown >= touchDelay && queue.size() < MAX_NUMBERS) {
                    lastTouchDown = System.currentTimeMillis();
                    queue.add(new FallingDigit((int)event.getX(), (int)event.getY(), dp40));
                    invalidate();
                }
                return true;
        }

        return false;
    }

    private class FallingDigit {
        int colorShift = 4;
        int x, y, value, transparency, size;
        String character;

        public FallingDigit(int x, int y, int size) {
            init();
            this.x = x - (dp40/4);
            this.y = y + (dp40/2);
            this.size = size;
        }

        public FallingDigit() {
            init();
            x = rng.nextInt(getWidth() - dp20);
            y = rng.nextInt(getHeight() - dp20);
            transparency = 0;
        }

        private void init() {
            value = rng.nextInt(9);
            transparency = 250;
            character = (chars != null) ? chars[value] : String.valueOf(value +1);
        }
    }


}
