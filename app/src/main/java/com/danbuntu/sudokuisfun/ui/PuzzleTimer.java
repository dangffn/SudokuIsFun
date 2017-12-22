package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Chronometer;

/**
 * Created by dan on 8/6/16. Have a great day!
 */
public class PuzzleTimer extends Chronometer {

    private Context mContext;
    private long mTotalDuration = 0;
    private long mSessionDuration = 0;
    private long mLastSaveDuration = 0;
    private long mSessionBase;
    private long mLastSaveBase;
    private boolean mRunning = false;
    private boolean mHalted = false;

    public PuzzleTimer(Context context) {
        this(context, null);
    }

    public PuzzleTimer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PuzzleTimer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
        initBaseDuration(0);
    }

    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setTextAppearance(android.R.style.TextAppearance_Large);
        } else {
            setTextAppearance(mContext, android.R.style.TextAppearance_Large);
        }
        setTextColor(Color.WHITE);
    }

    public void blink(int color) {
        setTextColor(color);
        AlphaAnimation blinkAnimation = new AlphaAnimation(0, 1);
        blinkAnimation.setDuration(200);
        blinkAnimation.setRepeatCount(5);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        startAnimation(blinkAnimation);
    }

    public void initBaseDuration(long totalDuration) {
        Log.i("PuzzleTimer", "Setting duration of " + totalDuration / 1000 + " seconds");
        mTotalDuration = totalDuration;
        mSessionDuration = 0;
        mLastSaveDuration = 0;
        mSessionBase = mLastSaveBase = SystemClock.elapsedRealtime();

        setBaseDuration(mTotalDuration);
    }

    private void setBaseDuration(long dur) {
        setBase(SystemClock.elapsedRealtime() - dur);
    }

    public void onSaveInstanceState(Bundle saveState) {
        if (saveState == null) return;

        Bundle bundle = new Bundle();
        bundle.putBoolean("HALTED", mHalted);
        bundle.putBoolean("RUNNING", mRunning);
        bundle.putLong("TOTAL_DURATION", mTotalDuration);
        bundle.putLong("SESSION_DURATION", mSessionDuration);
        bundle.putLong("LAST_SAVE_DURATION", mLastSaveDuration);

        bundle.putLong("SESSION_BASE", mSessionBase);
        bundle.putLong("LAST_SAVE_BASE", mLastSaveBase);

        saveState.putParcelable("PuzzleTimer", bundle);
    }

    public void onRestoreInstanceState(Bundle saveState) {
        if (saveState == null) return;

        Bundle bundle = saveState.getBundle("PuzzleTimer");
        if (bundle != null) {
            mHalted = bundle.getBoolean("HALTED", false);
            mRunning = bundle.getBoolean("RUNNING", false);
            mTotalDuration = bundle.getLong("TOTAL_DURATION", 0);
            mSessionDuration = bundle.getLong("SESSION_DURATION", 0);
            mLastSaveDuration = bundle.getLong("LAST_SAVE_DURATION", 0);

            mSessionBase = bundle.getLong("SESSION_BASE", 0);
            mLastSaveBase = bundle.getLong("LAST_SAVE_BASE", 0);
        }
    }

    public long getPresentDuration() {
        if (mRunning) {
            Log.i("PuzzleTimer", "Total duration is " + (SystemClock.elapsedRealtime() - getBase()) / 1000 + " seconds");
            return SystemClock.elapsedRealtime() - getBase();
        }
        Log.i("PuzzleTimer", "Total duration is " + mTotalDuration / 1000 + " seconds");
        return mTotalDuration;
    }

    public long getSessionDuration() {
        if(mRunning) {
            Log.i("PuzzleTimer", "Session duration is " + (SystemClock.elapsedRealtime() - mSessionBase) / 1000 + " seconds");
            return SystemClock.elapsedRealtime() - mSessionBase;
        }
        Log.i("PuzzleTimer", "Session duration is " + mSessionDuration / 1000 + " seconds");
        return mSessionDuration;
    }

    public long getAndResetLastSaveDuration() {
        long lastSaveDuration;

        if(mRunning) {
            lastSaveDuration =  SystemClock.elapsedRealtime() - mLastSaveBase;
        } else {
            lastSaveDuration = mLastSaveDuration;
        }

        Log.i("PuzzleTimer", "Last save duration is " + lastSaveDuration + " seconds");
        mLastSaveBase = SystemClock.elapsedRealtime();
        mLastSaveDuration = 0;

        return lastSaveDuration;
    }

    public void onPause() {
        if (mRunning) {
            mHalted = true;
            stop();
        }
    }

    public void onResume() {
        if (!mRunning && mHalted) {
            mHalted = false;
            start();
        }
    }

    public void postponedStart() {
        // triggers a start when onResume() is called
        if(!mRunning) mHalted = true;
    }

    @Override
    public void start() {
        Log.i("PuzzleTimer", "STARTING timer with duration of " + getPresentDuration() / 1000 + " seconds");
        if(!mRunning) {
            setTextColor(Color.WHITE);
            long realTime = SystemClock.elapsedRealtime();
            mSessionBase = realTime - mSessionDuration;
            mLastSaveBase = realTime - mLastSaveDuration;
            setBaseDuration(mTotalDuration);
            mRunning = true;
        }
        super.start();
    }

    @Override
    public void stop() {
        Log.i("PuzzleTimer", "STOPPING timer with duration of " + getPresentDuration() / 1000 + " seconds");
        if(mRunning) {
            mTotalDuration = getPresentDuration();
            mSessionDuration = getSessionDuration();
            mLastSaveDuration = getAndResetLastSaveDuration();
            mRunning = false;
        }
        super.stop();
    }
}
