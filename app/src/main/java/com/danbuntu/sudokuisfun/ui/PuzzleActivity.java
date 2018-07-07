package com.danbuntu.sudokuisfun.ui;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;
import com.danbuntu.sudokuisfun.puzzle.Puzzle;

public class PuzzleActivity extends AppCompatActivity implements ActionMode.Callback, ProgressFragment.ProgressCallbacks {

    final static String INTENT_START_TYPE = "START_TYPE";
    final static String INTENT_PUZZLE_DATA = "PUZZLE_DATA";
    final static String INTENT_IMAGE_PATH = "IMAGE_PATH";
    final static String INTENT_PUZZLE_DURATION = "PUZZLE_DURATION";
    final static String INTENT_PUZZLE_NAME = "TEMPLATE_NAME";
    final static String INTENT_SAVE_X = "SAVE_X";
    final static String INTENT_SAVE_Y = "SAVE_Y";
    final static String INTENT_SAVE_R = "SAVE_R";
    final static String INTENT_SAVE_ASSISTED = "INTENT_SAVE_ASSISTED";
    final static int PUZZLE_FROM_IMAGE = 2;
    final static int PUZZLE_FROM_TEMPLATE = 3;
    final static int PUZZLE_FROM_SAVE_STATE = 4;
    final static int NEW_BLANK_PUZZLE = 5;

    SudokuGridView gridView;
    ImageView preview;
    MenuItem menuUndo;
    boolean updatePossibilities, autoSave, startInEditMode = false;
    boolean imageLoadValid = false;
    boolean saveThumbnail;

    Bitmap previewBitmap;
    KeyboardFragment keyboardFragment;

    int START_TYPE;

    ProgressFragment mProgressFragment;

    String recognizePath;
    String puzzleName;
    String units;
    PuzzleTimer puzzleTimer;

    Puzzle puzzle;
    PuzzleManager pm;

    private Animation getFadeOutAnimation(final View view) {
        Animation.AnimationListener listener = new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(250);
        animation.setAnimationListener(listener);
        return animation;
    }

    private Animation getFadeInAnimation(final View view) {
        Animation.AnimationListener listener = new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };
        Animation animation = new AlphaAnimation(0, 1);
        animation.setDuration(250);
        animation.setAnimationListener(listener);
        return animation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        saveThumbnail = !PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_useDefaultPuzzleIcon), false);

        preview = (ImageView) findViewById(R.id.imgPuzzlePreview);

        gridView = (SudokuGridView) findViewById(R.id.sudokuGridView);

        puzzleTimer = new PuzzleTimer(this);

        android.app.ActionBar.LayoutParams params = new android.app.ActionBar.LayoutParams(android.app.ActionBar.LayoutParams.WRAP_CONTENT,
                android.app.ActionBar.LayoutParams.MATCH_PARENT);

        puzzleTimer.setLayoutParams(params);
        puzzleTimer.setGravity(Gravity.CENTER_VERTICAL);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(puzzleTimer);
            actionBar.setDisplayShowCustomEnabled(true);
        } else {
            Log.e("PuzzleActivity", "Could not get ActionBar to display the puzzleTimer");
        }

        Intent incomingIntent = getIntent();
        Bundle extras = incomingIntent.getExtras();

        puzzle = new Puzzle(gridView);

        if (extras != null) {
            // set the starting time to what we have saved for this puzzle
            long startTime = extras.getLong(INTENT_PUZZLE_DURATION, 0);
            if(startTime > 0) puzzleTimer.initBaseDuration(extras.getLong(INTENT_PUZZLE_DURATION, 0));

            switch (START_TYPE = extras.getInt(INTENT_START_TYPE, -1)) {
                case (PUZZLE_FROM_TEMPLATE):
                    puzzle.loadPuzzleData(extras.getIntArray(INTENT_PUZZLE_DATA));
                    puzzle.setLogMode(true);
                    puzzleTimer.postponedStart();
                    break;
                case (PUZZLE_FROM_SAVE_STATE):
                    short revisionNumber = extras.getShort(INTENT_SAVE_R, (short) 0);
                    String puzzleData = extras.getString(INTENT_PUZZLE_DATA);
                    puzzle.loadPuzzleData(puzzleData, revisionNumber);
                    puzzle.setLogMode(true);
                    puzzleTimer.postponedStart();
                    break;
                case (NEW_BLANK_PUZZLE):
                    puzzle = new Puzzle(gridView);
                    puzzle.setLogMode(false);
                    startInEditMode = true;
                    break;
                case (PUZZLE_FROM_IMAGE):
                    // if we have an image path in the intent, then were going to be loading a puzzle from a picture
                    if (extras.getString(INTENT_IMAGE_PATH) != null) {
                        startInEditMode = true;
                        recognizePath = extras.getString(INTENT_IMAGE_PATH);
                        getIntent().removeExtra(INTENT_IMAGE_PATH);
                    }
                    break;
            }
            getIntent().removeExtra(INTENT_START_TYPE);

            puzzleName = extras.getString(INTENT_PUZZLE_NAME, null);

            // whether or not the previous save was assisted
            puzzle.setComputerAssist(extras.getBoolean(INTENT_SAVE_ASSISTED));

            int x = extras.getInt(INTENT_SAVE_X, -1);
            int y = extras.getInt(INTENT_SAVE_Y, -1);

            gridView.setSelectedCellCoords(x, y);
        }

        puzzle.setPuzzleListener(new Puzzle.PuzzleListener() {
            ProgressDialog pd;

            @Override
            public void undoNotAvailable() {
                showUndoButton(false);
            }

            @Override
            public void undoAvailable() {
                showUndoButton(true);
            }

            @Override
            public void puzzleSolveStarted() {
                pd = new ProgressDialog(PuzzleActivity.this);
                pd.setTitle("Solving");
                pd.setMessage("Solving the puzzle");
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.show();
            }

            @Override
            public void puzzleSolveEnded() {
                gridView.invalidate();
                if (pd != null) pd.dismiss();
            }

            @Override
            public void puzzleSolved() {
                if (!gridView.inEditMode())
                    Toast.makeText(PuzzleActivity.this, "Puzzle Solved", Toast.LENGTH_LONG).show();
                puzzleTimer.stop();
                puzzleTimer.blink(Color.GREEN);

                pm.logFinishTime(puzzleName, puzzleTimer.getPresentDuration(), puzzle.didComputerAssist());

                if(puzzle.getHintCount() > 0) {
                    Toast.makeText(PuzzleActivity.this, "Fixed " + puzzle.getHintCount() + " mistakes (highlighted in orange)", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void puzzleResumed() {
                puzzleTimer.start();

            }

            @Override
            public void puzzleImpossible() {
                new AlertDialog.Builder(PuzzleActivity.this)
                        .setTitle("Could not solve")
                        .setMessage("This puzzle is impossible")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                puzzleTimer.blink(Color.RED);
            }

            @Override
            public void puzzleError() {
                new AlertDialog.Builder(PuzzleActivity.this)
                        .setTitle("Could not solve")
                        .setMessage("There is an error in this puzzle, please fix and try to solve again")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                gridView.invalidate();
            }
        });

        // set a default name that doesn't already exist in the database
        pm = PuzzleManager.getInstance(this);
        if (puzzleName == null) {
            puzzleName = pm.nextAvailableName("Unnamed");
        }

        setTitle(puzzleName);

        // if we are in landscape mode, set the activity to fullscreen so theres more room for the gridView
        switch (getResources().getConfiguration().orientation) {
            case (Configuration.ORIENTATION_LANDSCAPE):
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
            case (Configuration.ORIENTATION_PORTRAIT):
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String smallKeyboardSize = getString(R.string.pref_key_keyboardSizeSmall);
        String keyboardSize = smallKeyboardSize;
        if (prefs != null)
            keyboardSize = prefs.getString(getString(R.string.pref_key_keyboardSize), smallKeyboardSize);
        int keyboardLayout = (keyboardSize.equals(smallKeyboardSize)) ? R.layout.layout_keyboard_small : R.layout.layout_keyboard_large;

        FragmentManager fm = getFragmentManager();
        keyboardFragment = (KeyboardFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (keyboardFragment == null || keyboardLayout != keyboardFragment.getLayoutId()) {
            Log.i("FRAGMENT", "NEW FRAGMENT");
            keyboardFragment = KeyboardFragment.newInstance(keyboardLayout);
        } else {
            Log.i("FRAGMENT", "REUSED FRAGMENT");
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.fragmentContainer, keyboardFragment);

        if (startInEditMode) {
            if (recognizePath != null) {

                mProgressFragment = (ProgressFragment) fm.findFragmentByTag("PROG");
                if (mProgressFragment == null) {
                    mProgressFragment = ProgressFragment.newInstance(recognizePath);
                    ft.add(mProgressFragment, "PROG");
                }
                mProgressFragment.setCancelable(false);

            }

        }

        ft.commit();

        keyboardFragment.setKeyPressListener(new KeyboardFragment.KeyPressListener() {
            final Animation fadeIn = getFadeInAnimation(preview);
            final Animation fadeOut = getFadeOutAnimation(preview);

            @Override
            public void numberPressed(int value, boolean setValue) {
                SudokuGridView.Coord coord = gridView.getSelectedCellCoords();
                if (setValue) {
                    puzzle.setCell(coord.getX(), coord.getY(), value, gridView.inEditMode(), updatePossibilities);
                } else {
                    puzzle.togglePossibility(coord.getX(), coord.getY(), value);
                }
                gridView.invalidate();
            }

            @Override
            public void clearPressed() {
                SudokuGridView.Coord coord = gridView.getSelectedCellCoords();
                puzzle.clearCell(coord.getX(), coord.getY(), gridView.inEditMode());
                gridView.invalidate();
            }

            @Override
            public void previewPressed() {
                preview.startAnimation(fadeIn);
            }

            @Override
            public void previewUnpressed() {
                preview.startAnimation(fadeOut);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_puzzle, menu);
        menuUndo = menu.findItem(R.id.menu_undo);
        if (puzzle != null && puzzle.getCurrentRevision() > 0) menuUndo.setVisible(true);

        if(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_hideAssistanceOptions), false)) {

            menu.findItem(R.id.menu_solve).setVisible(false);
            menu.findItem(R.id.menu_find_possibilities).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.menu_clear):
                puzzle.clearPuzzle(false);
                gridView.invalidate();
                break;
            case (R.id.menu_save_template):
                saveTemplate();
                break;
            case (R.id.action_settings):
                showPreferencesActivity();
                break;
            case (R.id.menu_solve):
                if (puzzle.isSolved()) {
                    Toast.makeText(this, getString(R.string.message_alreadySolved), Toast.LENGTH_LONG).show();
                } else {
                    puzzle.doSolve();
                }
                break;
            case (R.id.menu_save_progress):
                saveProgress(true);
                break;
            case (R.id.menu_undo):
                puzzle.revert();
                gridView.invalidate();
                break;
            case (R.id.menu_find_possibilities):
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        puzzle.displayAllPossibilities();
                        gridView.post(new Runnable() {
                            @Override
                            public void run() {
                                gridView.invalidate();
                            }
                        });
                    }
                }).start();
                break;
            case(R.id.menu_show_errors):
                showErrors();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i("PuzzleActivity", "OnSaveInstanceState");

        outState.putInt("START_TYPE", START_TYPE);
        outState.putBundle("PUZZLESTATE", puzzle.onSaveInstanceState());
        outState.putString("TEMPLATENAME", puzzleName);
        outState.putInt("X", gridView.getSelectedCellCoords().getX());
        outState.putInt("Y", gridView.getSelectedCellCoords().getY());

        // durations
        puzzleTimer.onSaveInstanceState(outState);

        outState.putBoolean("LOGMODE", puzzle.inLogMode());
        outState.putBoolean("EDITMODE", startInEditMode);
        outState.putParcelable("PREVIEWBITMAP", previewBitmap);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("PuzzleActivity", "OnRestoreInstanceState");

        if (savedInstanceState != null) {

            START_TYPE = savedInstanceState.getInt("START_TYPE");
            puzzle.onRestoreInstanceState(savedInstanceState.getBundle("PUZZLESTATE"));
            puzzleName = savedInstanceState.getString("TEMPLATENAME");
            gridView.setSelectedCellCoords(savedInstanceState.getInt("X", -1), savedInstanceState.getInt("Y", -1));

            // durations
            puzzleTimer.onRestoreInstanceState(savedInstanceState);

            puzzle.setLogMode(savedInstanceState.getBoolean("LOGMODE"));
            startInEditMode = savedInstanceState.getBoolean("EDITMODE", false);
            previewBitmap = savedInstanceState.getParcelable("PREVIEWBITMAP");
            preview.setImageBitmap(previewBitmap);

        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        puzzleTimer.onPause();

        if(puzzle.isSolving()) {
            puzzle.cancelSolving();
        }

        // don't save if:
        //   1. the preference says not to
        //   2. or we are loading from an image, but the image load wasn't valid
        if (autoSave && (START_TYPE != PUZZLE_FROM_IMAGE || imageLoadValid))
            saveProgress(isFinishing());
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs != null) {

            // gridView specific settings
            gridView.setHighlightNeighbors(prefs.getBoolean(getString(R.string.pref_key_highlightNeighbors), true));
            gridView.setHighlightNumbers(prefs.getBoolean(getString(R.string.pref_key_highlightNumbers), true));
            updatePossibilities = prefs.getBoolean(getString(R.string.pref_key_updatePossibilities), true);
            autoSave = prefs.getBoolean(getString(R.string.pref_key_autoSave), true);

            // puzzle specific settings
            puzzle.showConflicts(prefs.getBoolean(getString(R.string.pref_key_showConflicts), true));

            // update the units that the gridView and keyboardFragment should be displaying
            units = prefs.getString(getString(R.string.pref_key_characterUnit), getString(R.string.pref_key_characterUnitDigits));
            int unitMode = SudokuGridView.MODE_NUMBERS;
            if (units.equals(getString(R.string.pref_key_characterUnitColors))) {
                unitMode = SudokuGridView.MODE_COLORS;
            } else if (units.equals(getString(R.string.pref_key_characterUnitLetters))) {
                unitMode = SudokuGridView.MODE_LETTERS;
            }
            gridView.setDisplayMode(unitMode);
            keyboardFragment.setDisplayMode(unitMode);
        }

        showUndoButton(puzzle.getCurrentRevision() > 0);

        puzzleTimer.onResume();

        gridView.invalidate();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if(startInEditMode)
            startSupportActionMode(this);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_puzzle_cab, menu);
        mode.setTitle("Puzzle Editor");

        puzzle.setLogMode(false);
        gridView.setEditMode(true);
        keyboardFragment.setEditMode(true);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        imageLoadValid = false;

        switch (item.getItemId()) {
            case (R.id.menu_cab_good):
                if (!puzzle.isValid(true)) {
                    // tell the user to fix the errors before we close action mode
                    new AlertDialog.Builder(this)
                            .setTitle("Errors in the puzzle")
                            .setMessage("There were errors found in this puzzle, please fix them before saving")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                } else if (puzzle.isSolved()) {
                    Toast.makeText(PuzzleActivity.this, getString(R.string.message_alreadySolved), Toast.LENGTH_LONG).show();
                } else {
                    imageLoadValid = true;
                    puzzleTimer.start();
                    mode.finish();

                    // get a name that doesn't exist already
                    puzzleName = pm.nextAvailableName(puzzleName);

                    // notify the parent which tab this puzzle was saved to
                    saveTemplate(puzzleName);
                    Intent puzzleCreatedIntent = new Intent();
                    puzzleCreatedIntent.putExtra(getString(R.string.intent_extra_tab), getString(R.string.puzzle_category_DEFAULT));
                    setResult(RESULT_OK, puzzleCreatedIntent);
                }
                break;

            case (R.id.menu_cab_bad):
                puzzle.clearPuzzle(true);
                gridView.invalidate();
                mode.finish();
                break;

            case(R.id.menu_cab_obfuscate):
                puzzle.obfuscatePuzzle();
                gridView.invalidate();
                break;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pm.close();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        puzzle.setLogMode(true);
        keyboardFragment.setEditMode(false);
        gridView.setEditMode(false);
        startInEditMode = false;

        if (!isChangingConfigurations()) {
            // clear the preview image if the action mode is done, but not if configs are changing
            clearPreviewImage();
            if (!imageLoadValid) {
                // if the user didn't approve the image load, close the activity
                finish();
            }
        }
    }

    @Override
    public void onFragmentComplete(int[] array, Bitmap image) {
        previewBitmap = image;
        if (array != null) {
            if (preview != null) {
                preview.setImageBitmap(previewBitmap);
            }
            puzzle.loadPuzzleData(array);

            puzzle.isValid(true);
            gridView.invalidate();
        }

        if(previewBitmap != null) {
            keyboardFragment.setImageLoaded(true);

            if(!getFirstPuzzleLoadPreference()) showFirstImageLoadDialog();
        }

        // clear the cache since we don't need the temp images anymore
        SudokuUtils.clearAppCache(this);
    }

    private boolean getFirstPuzzleLoadPreference() {
        boolean firstImageLoad = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_loadedFirstImage), false);

        // if a puzzle hasn't been loaded form an image before return false, but set it to true in the preferences
        if(!firstImageLoad) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(getString(R.string.pref_key_loadedFirstImage), true)
                    .apply();
        }

        return firstImageLoad;
    }

    private void showErrors() {
        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Puzzle checkPuzzle = Puzzle.from(puzzle);
                checkPuzzle.doSolve(false, true);
                puzzle.matchCells(checkPuzzle);

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if(puzzle.getHintCount() <= 0) {

                            Toast.makeText(PuzzleActivity.this, "No errors found", Toast.LENGTH_SHORT)
                                    .show();

                        } else {

                            int errors = puzzle.getHintCount();
                            Toast.makeText(PuzzleActivity.this, errors + "errors found", Toast.LENGTH_SHORT)
                                    .show();
                            gridView.invalidate();

                        }

                    }
                });
            }
        }).start();
    }

    private void showFirstImageLoadDialog() {
        new AlertDialog.Builder(this)
                .setTitle("How does this look?")
                .setMessage("You just loaded your first puzzle from an image." +
                        "\n\nMake sure to fix any numbers that may have been incorrectly recognized before hitting the check mark at the top to add this new puzzle." +
                        "\n\nYou can also press the eye icon on the keyboard to show the image you just loaded so you can make sure everything is correct.")
                .setPositiveButton(getString(R.string.button_got_it), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create().show();
    }

    private void showUndoButton(boolean hide) {
        if (menuUndo != null) {
            menuUndo.setEnabled(hide);
            menuUndo.setVisible(hide);
        } else {
            Log.e("PuzzleActivity", "menuUndo is null but there was a visible change requested");
        }
    }

    private void clearPreviewImage() {
        if (previewBitmap != null) {
            if (!previewBitmap.isRecycled()) previewBitmap.recycle();
            previewBitmap = null;
        }
        preview.setImageBitmap(null);
    }

    private void saveProgress(boolean toast) {
        SudokuGridView.Coord coord = gridView.getSelectedCellCoords();

        int iconSize = getResources().getInteger(R.integer.icon_size);
        final Bitmap icon = (saveThumbnail) ? gridView.getSnapshot(iconSize, true) : null;

        boolean saved = pm.saveState(icon, puzzleName, puzzle.getCellArray(), coord.getX(), coord.getY(),
                puzzleTimer.getSessionDuration(), puzzleTimer.getAndResetLastSaveDuration(),
                puzzle.getCurrentRevision(), puzzle.isFinished(), puzzle.didComputerAssist());

        if(toast) {
            if (saved) {
                Toast.makeText(this, "Puzzle saved", Toast.LENGTH_SHORT).show();
                if (!isChangingConfigurations() && !isFinishing() && icon != null) animateSnapshot(icon);

            } else {
                Toast.makeText(this, "Puzzle not saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void animateSnapshot(final Bitmap icon) {
        if(icon == null || icon.isRecycled()) return;
        Paint paint = new Paint();
        paint.setColorFilter(new LightingColorFilter(Color.WHITE, -100));
        Canvas canvas = new Canvas(icon);
        canvas.drawBitmap(icon, 0, 0, paint);

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                preview.setImageBitmap(icon);
                preview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                preview.setVisibility(View.GONE);
                preview.setImageBitmap(null);
//                        snapshot.recycle();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation alphaAnim = new AlphaAnimation(1, 0);
        alphaAnim.setDuration(750);
        alphaAnim.setAnimationListener(listener);
        preview.startAnimation(alphaAnim);
    }

    public void showPreferencesActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void saveTemplate() {

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Name this puzzle");
        ab.setMessage("Type a name for this puzzle");
        LinearLayout ll = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_text_box, null);
        ab.setView(ll);

        final EditText et = (EditText) ll.findViewById(R.id.dialog_text_box);
        et.setHint(puzzleName);

        ab.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {

                // trying to save this current template to the database
                final String newName = (et.getText().toString().trim().equals("")) ? puzzleName : et.getText().toString().trim();

                if (pm.containsPuzzle(newName)) {

                    // the name the user wants is already saved, ask them if they want to overwrite the existing puzzle
                    new AlertDialog.Builder(PuzzleActivity.this)
                            .setTitle("Name Exists")
                            .setMessage("Do you want to overwrite the puzzle " + newName + "?")
                            .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface overWriteDialog, int which) {
                                    // the user wants to overwrite the existing puzzle
                                    saveTemplate(newName);

                                    overWriteDialog.dismiss();
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface overWriteDialog, int which) {

                                    // the user does not want to overwrite the existing puzzle
                                    overWriteDialog.dismiss();

                                }
                            })
                            .create().show();

                } else {

                    // the name the user wants is free, save it
                    saveTemplate(newName);

                    dialog.dismiss();

                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // user doesn't want to save anything
                dialog.cancel();

            }
        });

        ab.create().show();
    }

    private void saveTemplate(String nameToSave) {

        int iconSize = getResources().getInteger(R.integer.icon_size);
        Bitmap bmp = (saveThumbnail) ? gridView.getSnapshot(iconSize, false) : null;

        // the name the user wants is free, save it
        puzzleName = (nameToSave.trim().equals("")) ? puzzleName : nameToSave.trim();
        setTitle(puzzleName);

        String description = String.format(getString(R.string.created_on), SudokuUtils.getDate());

        pm.saveNewPuzzle(bmp,
                puzzleName,
                description,
                puzzle.getCellArray(),
                getString(R.string.puzzle_category_DEFAULT),
                START_TYPE != NEW_BLANK_PUZZLE);

        Toast.makeText(this, "Saved to " + getString(R.string.puzzle_category_myPuzzles), Toast.LENGTH_SHORT).show();
    }
}
