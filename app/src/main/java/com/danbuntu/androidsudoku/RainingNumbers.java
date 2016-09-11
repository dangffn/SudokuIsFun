package com.danbuntu.androidsudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Dan on 5/28/2016.
 */
public class RainingNumbers extends SurfaceView implements SurfaceHolder.Callback {
    int MAX_NUMBERS = 100;
    Random rand;
    Queue<FallingDigit> queue;
    boolean doRain;
    Paint textPaint;
    Thread addNumbers;
    GraphicThread update;
    int bgColor;
    int dp40, dp20, g;
    int index = -1;
    Context context;
    ArrayList<Integer> rnd;

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

    public void stop() {
        if (update != null) update.stopThread();
        doRain = false;

        update = null;
        addNumbers = null;
    }

    private void init() {
        queue = new ConcurrentLinkedQueue<>();
        dp40 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        dp20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        bgColor = getResources().getColor(R.color.light_blue);
        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf"));

        rand = new Random();
        rnd = new ArrayList<>();

        g = dp20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        getHolder().addCallback(this);

    }

    public void start() {

        addNumbers = new Thread(new Runnable() {
            @Override
            public void run() {
                while (doRain) {
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

        update = new GraphicThread(this);

        doRain = true;

        addNumbers.start();
        update.startThread();

    }

    public void applyGravity() {
        for (FallingDigit drop : queue) {
//            drop.y += drop.g;
            if (drop.c >= 150) drop.g = -drop.g;
            drop.c += drop.g;
            drop.y += g;
        }


    }

    private void drawDigit(Canvas canvas, FallingDigit digit) {
        textPaint.setTextSize(digit.s);
        textPaint.setColor(digit.c << 24);
        canvas.drawText(String.valueOf(digit.n), digit.x, digit.y, textPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(bgColor);

        for (FallingDigit drop : queue) {
            // remove the digit if its off screen
//            if (drop.y > getHeight() + drop.s) {
            if(drop.c <= 0 || drop.y > getBottom() + drop.s) {
                queue.remove(drop);
            } else {
                drawDigit(canvas, drop);
            }
        }

        super.onDraw(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("RainingNumbers", "Surface created");
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public int getRandomX() {
        if (rnd.size() == 0) {
            int increment = getWidth() / 20;
            for (int i = 0; i <= getWidth(); i += increment) {
                rnd.add(i);
            }
            Collections.shuffle(rnd);
        }

        index++;
        if (index >= rnd.size()) {
            Collections.shuffle(rnd);
            index = 0;
        }

        return rnd.get(index);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    private class FallingDigit {
        int x, y, n, s, g, c;

        public FallingDigit() {
            n = rand.nextInt(9) + 1;

            // minimum size is 10dp, maximum size is about 60dp
//            s = (rand.nextInt(5) * (dp40 / 5)) + dp20;
            s = dp40 + dp20;

            // don't render numbers more than half way off screen horizontally
//            x = rand.nextInt(getWidth()) - (s / 2);
            x = getRandomX() - (s / 2);

            // sets color to a semi-transparent black
            // don't let the transparency fall below 50 otherwise it would basically be invisible
//            int transparency = (int) (200f / (dp40) * (s - dp20));
//            c = Math.max(50, transparency);

            c = 0;

//            Log.i("COLOR", Integer.toHexString(c));

//            g = Math.max(1, s / 20);
            g = 4;
            y = rand.nextInt(getHeight() - (s/2));
        }
    }
}
