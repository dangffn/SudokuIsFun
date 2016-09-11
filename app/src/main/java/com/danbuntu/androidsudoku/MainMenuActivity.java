package com.danbuntu.androidsudoku;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Dan on 5/28/2016.
 */
public class MainMenuActivity extends AppCompatActivity {
    RainingNumbers rain;
    Button btnLoadLast;

    //TODO add a default puzzle database to the assets

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        final Button btnSudoku = (Button)findViewById(R.id.btnStartSudoku);
        final Button btnSettings = (Button)findViewById(R.id.btnSettings);
        btnLoadLast = (Button)findViewById(R.id.btnLoadLastGame);

        rain = (RainingNumbers) findViewById(R.id.rainingNumbers);

        Animation fadeIn = new AlphaAnimation(0x00, 0xFF);
        fadeIn.setDuration(25000);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (btnSudoku != null) {
                    btnSudoku.setVisibility(View.VISIBLE);
                }
                if (btnSettings != null) {
                    btnSettings.setVisibility(View.VISIBLE);
                }
                if (btnLoadLast != null) {
                    btnLoadLast.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        if (btnSudoku != null) {
            btnSudoku.startAnimation(fadeIn);
        }
        if (btnSettings != null) {
            btnSettings.startAnimation(fadeIn);
        }
        if(btnLoadLast != null) {
            btnLoadLast.startAnimation(fadeIn);
        }
    }

    @Override
    protected void onResume() {
        //TODO Rain doesn't restart when resuming activity from screen off, check to see if this is fixed

        String lastPuzzle = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_last_puzzle), null);
        if(lastPuzzle != null && new TemplateManager(this).containsSave(lastPuzzle)) {
            btnLoadLast.setEnabled(true);
        } else {
            btnLoadLast.setEnabled(false);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(getString(R.string.pref_last_puzzle), null)
                    .apply();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        rain.stop();
        super.onPause();
    }

    public void startNewGameActivity(View view) {
        Intent intent = new Intent(MainMenuActivity.this, NewGameActivity.class);
        startActivity(intent);
    }

    public void startSettingsActivity(View view) {
        Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void loadLastGame(View view) {
        //TODO test this

        TemplateManager tm = new TemplateManager(this);
        String lastPuzzle = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_last_puzzle), null);
        String saveState = tm.getSave(lastPuzzle);

        if(saveState == null) {

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(getString(R.string.pref_last_puzzle), null)
                    .apply();
            Button btnLast = (Button)findViewById(R.id.btnLoadLastGame);
            if(btnLast != null) btnLast.setEnabled(false);
            Toast.makeText(this, "Failed to load last save", Toast.LENGTH_LONG).show();

        } else {

            Intent intent = new Intent(this, PuzzleActivity.class);
<<<<<<< HEAD

//            Bundle bundle = new Bundle();
//            bundle.putInt(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_SAVE_STATE);
//            bundle.putString(PuzzleActivity.INTENT_PUZZLE_DATA, saveState);
//            bundle.putString(PuzzleActivity.INTENT_TEMPLATE_NAME, lastPuzzle);
//            intent.putExtras(bundle);

=======
>>>>>>> 2c63def... Fixed alot of stuff
            intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_SAVE_STATE);
            intent.putExtra(PuzzleActivity.INTENT_PUZZLE_DATA, saveState);
            intent.putExtra(PuzzleActivity.INTENT_TEMPLATE_NAME, lastPuzzle);
            startActivity(intent);

        }
    }
}
