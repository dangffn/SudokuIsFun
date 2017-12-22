package com.danbuntu.sudokuisfun.ui;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

/**
 * Created by Dan on 5/28/2016. Have a great day!
 */
public class MainMenuActivity extends AppCompatActivity {

    private RainingNumbers rain;
    private PuzzleManager pm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        rain = (RainingNumbers) findViewById(R.id.rainingNumbers);
    }

    @Override
    protected void onStart() {
        pm = PuzzleManager.getInstance(this);

        Button btnLoadLast = (Button) findViewById(R.id.btnLoadLastGame);
        if (btnLoadLast != null) btnLoadLast.setEnabled(hasLastPuzzle());

        final int[] ids = {R.id.btnStartSudoku, R.id.btnSettings, R.id.btnLoadLastGame, R.id.btnLoadStatistics};

        Animation fadeIn = new AlphaAnimation(0x00, 0xFF);
        fadeIn.setDuration(2500);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                for (int i : ids) {
                    View view = findViewById(i);
                    if (view != null) view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        for (int i : ids) {
            View view = findViewById(i);
            if (view != null) view.startAnimation(fadeIn);
        }

        // set raining numbers to reflect the display settings
        rain.setChars(null);
        rain.setColors(null);
        String characterPreference = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_key_characterUnit), null);
        if (characterPreference != null && characterPreference.equals(getString(R.string.pref_key_characterUnitLetters))) {
            rain.setChars(getResources().getStringArray(R.array.letters));
        } else if (characterPreference != null && characterPreference.equals(getString(R.string.pref_key_characterUnitColors))) {
            rain.setColors(getResources().getIntArray(R.array.colors));
        }

        // start the rain
        rain.start();

        Log.i("MainMenuActivity", "onStart called");
        super.onStart();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected void onStop() {
        if (pm != null) pm.close();

        rain.stop();

        Log.i("MainMenuActivity", "onStop called");
        super.onStop();
    }

    private boolean hasLastPuzzle() {
        String lastPuzzle = SudokuUtils.getLastSavePreference(this);
        if (pm != null && lastPuzzle != null) {
            if (pm.containsSave(lastPuzzle)) {
                return true;
            } else {
                // clear the preference if its invalid
                SudokuUtils.setLastSavePreference(this, null);
                return false;
            }
        }
        return false;
    }

    public void startNewGameActivity(View view) {
        Intent intent = new Intent(MainMenuActivity.this, NewGameActivity.class);
        startActivity(intent);
    }

    public void loadLastGame(View view) {

        String lastPuzzle = SudokuUtils.getLastSavePreference(this);

        if (lastPuzzle == null) return;

        Bundle saveState = pm.getSaveState(lastPuzzle);

        if (saveState != null) {

            // start the PuzzleActivity
            Intent intent = new Intent(this, PuzzleActivity.class);
            intent.putExtras(saveState);
            startActivity(intent);

        } else {

            // save state for this puzzle doesn't exists, delete the preference
            SudokuUtils.setLastSavePreference(this, null);
            Button btnLast = (Button) findViewById(R.id.btnLoadLastGame);
            if (btnLast != null) btnLast.setEnabled(false);
            Toast.makeText(this, "Failed to load last save", Toast.LENGTH_LONG).show();
            Log.e("MainMenuActivity", "Failed to find the last saved puzzle state");

        }

    }

    public void startStatisticsActivity(View view) {
        Intent intent = new Intent(MainMenuActivity.this, StatisticsActivity.class);
        startActivity(intent);
    }

    public void startSettingsActivity(View view) {
        Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
        startActivity(intent);
    }
}
