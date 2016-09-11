package com.danbuntu.sudokuisfun.ui;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by Dan on 6/1/2016.
 */
public class GraphicThread extends Thread {
    private final static int SLEEP_TIME = 40;

    private boolean running = false;
    private RainingNumbers parent = null;
    private final SurfaceHolder surfaceHolder;

    public GraphicThread(RainingNumbers parent) {
        super();
        this.parent = parent;
        this.surfaceHolder = this.parent.getHolder();
    }

    public void startThread() {
        Log.i("GraphicThread", "Starting");
        running = true;
        super.start();
    }

    public void stopThread() {
        Log.i("GraphicThread", "Stopping");
        running = false;
    }

    @Override
    public synchronized void start() {
        running = true;
        super.start();
    }

    @SuppressLint("WrongCall")
    public void run() {
        Canvas c = null;
        while (running) {

            try {

                c = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    if (c != null) {
                        parent.applyGravity();
                        parent.onDraw(c);
                    }
                }
                sleep(SLEEP_TIME);

            } catch (InterruptedException ie) {
                Log.e("GraphicThread", "Interrupted");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                if (c != null) {
                    surfaceHolder.unlockCanvasAndPost(c);
                }

            }
        }
    }
}
