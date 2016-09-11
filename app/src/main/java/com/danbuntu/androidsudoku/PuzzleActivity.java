package com.danbuntu.androidsudoku;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class PuzzleActivity extends AppCompatActivity {
    Cell[][] rows, cols, blks;
    Cell[] cells;
    int[] backup;
    boolean VALUE_SET = true;
    SudokuGridView gridView;

    final static String INTENT_START_TYPE = "START_TYPE";
    final static String INTENT_PUZZLE_DATA = "PUZZLE_DATA";
    final static String INTENT_IMAGE_PATH = "IMAGE_PATH";
    final static String INTENT_TEMPLATE_NAME = "TEMPLATE_NAME";

    final static int PUZZLE_FROM_TEMPLATE = 3;
    final static int PUZZLE_FROM_SAVE_STATE = 4;

    boolean updatePossibilities, autoSave;

    final static short SOLVED = 0;
    final static short IMPOSSIBLE = -2;
    final static short UNSOLVED = -1;

    boolean doRecognize = false;
    String recognizePath;
    String templateName;

    //TODO Number buttons should have an outline on the font

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create blank cells
        initializeCells();

        gridView = (SudokuGridView) findViewById(R.id.sudokuGridView);
        if (gridView != null) gridView.bindToCells(rows);

        Intent incomingIntent = getIntent();
        Bundle extras = incomingIntent.getExtras();
        if (extras != null) {

            switch (extras.getInt(INTENT_START_TYPE, -1)) {
                case (PUZZLE_FROM_TEMPLATE):
                    loadPuzzleData(extras.getIntArray(INTENT_PUZZLE_DATA));
                    break;
                case (PUZZLE_FROM_SAVE_STATE):
                    loadPuzzleData(extras.getString(INTENT_PUZZLE_DATA));
                    break;
            }

            if(extras.getString(INTENT_IMAGE_PATH) != null) {
                doRecognize = true;
                recognizePath = extras.getString(INTENT_IMAGE_PATH);
            }

            templateName = extras.getString(INTENT_TEMPLATE_NAME, "Unnamed");

        } else {
            templateName = "Unnamed";
        }

        setTitle(templateName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_puzzle, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.menu_clear):
                softClear();
                break;
            case (R.id.menu_toggle_lock):
                toggleCellLock();
                break;
            case (R.id.menu_save_template):
                saveTemplate();
                break;
            case (R.id.action_settings):
                showPreferencesActivity();
                break;
            case (R.id.menu_solve):
                doSolve();
                break;
            case (R.id.menu_save_progress):
                saveProgress();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveProgress() {
        TemplateManager tm = new TemplateManager(this);
        SudokuGridView.Coord coord = gridView.getSelectedCellCoords();
        tm.saveState(getGridViewBitmap(true), templateName, cells, coord.getX(), coord.getY());
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putString(getString(R.string.pref_last_puzzle), templateName)
                .apply();
    }

    @Override
    protected void onPause() {
        if (autoSave) saveProgress();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs != null) {
            gridView.setHighlightNeighbors(prefs.getBoolean(getString(R.string.pref_highlight_neighbors), true));
            gridView.setHighlightNumbers(prefs.getBoolean(getString(R.string.pref_highlight_numbers), true));
            updatePossibilities = prefs.getBoolean(getString(R.string.pref_update_possibilities), true);
            autoSave = prefs.getBoolean(getString(R.string.pref_auto_save), true);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // this has to run after the activity is loaded so it cant go in onActivityResult
        if (doRecognize && recognizePath != null) {
            final OCRData ocrData = new OCRData(PuzzleActivity.this, recognizePath);

            final ProgressDialog progress = new ProgressDialog(PuzzleActivity.this);
            progress.setTitle("Recognizing");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setMax(GridSpecs.ROWS * GridSpecs.COLS);
            progress.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ocrData.beginScan(progress);

                    loadPuzzleData(ocrData.grid);

                    doRecognize = false;
                    recognizePath = null;

                    gridView.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            gridView.invalidate();
                        }
                    });
                }
            }).start();
        }
    }

    public void showPreferencesActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private Bitmap getGridViewBitmap(boolean preserve) {

        Bitmap bmp = Bitmap.createBitmap(gridView.getWidth(), gridView.getHeight(), Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bmp);

        if (!preserve) {
            // grab an image of the sudoku grid without any highlight effects (for the template icon)
//            SudokuGridView.Coord oldCoord = gridView.getSelectedCellCoords();
//            gridView.coord = new SudokuGridView.Coord();
            gridView.drawBlankGrid(canvas);
//            gridView.coord = oldCoord;
        } else {
            // grab an image of the sudoku grid as it is (for the game save icon)
            gridView.draw(canvas);
        }

        bmp = Bitmap.createScaledBitmap(bmp, 200, 200, true);

        return bmp;
    }

    private void saveTemplate() {

        final TemplateManager tm = new TemplateManager(this);

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Name this puzzle");
        ab.setMessage("Type a name for this puzzle");
        LinearLayout ll = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_text_box, null);
        ab.setView(ll);

        final EditText et = (EditText) ll.findViewById(R.id.dialog_text_box);
        et.setHint(templateName);
        et.setText(templateName);

        ab.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {

                // trying to save this current template to the database
                final Bitmap bmp = getGridViewBitmap(false);
                final String newName = (et.getText().toString().trim().equals("")) ? templateName : et.getText().toString().trim();

                if (tm.containsTemplateName(newName)) {

                    // the name the user wants is already saved, ask them if they want to overwrite the existing puzzle
                    new AlertDialog.Builder(PuzzleActivity.this).setTitle("Name Exists").setMessage("Do you want to overwrite this template?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface overWriteDialog, int which) {

                            // the user wants to overwrite the existing puzle
                            // update the database and close the dialogs
                            templateName = newName;
                            tm.updateTemplate(bmp, templateName, cells);
                            setTitle(templateName);
                            overWriteDialog.dismiss();
                            dialog.dismiss();

                        }
                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface overWriteDialog, int which) {

                            // the user does not want to overwrite the existing puzzle
                            overWriteDialog.dismiss();

                        }
                    }).create().show();

                } else {

                    // the name the user wants is free, save it
                    templateName = newName;

                    Calendar cal = Calendar.getInstance();
                    Date currentLocalTime = cal.getTime();
                    DateFormat date = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");
                    String localTime = date.format(currentLocalTime);

                    tm.saveTemplate(bmp, templateName, "Created on " + localTime, cells);
                    setTitle(templateName);
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

    private void toggleCellLock() {
        for (Cell cell : cells)
            cell.lock(!cell.isLocked());

        gridView.invalidate();
    }

    private void savePuzzleState() {
//        backup = Arrays.copyOf(cells, cells.length);
        backup = new int[cells.length];
        for (int i = 0; i < cells.length; i++) {
            backup[i] = cells[i].getValue();
        }
    }

    private void restorePuzzleState() {
        if (backup != null) {
            for (int i = 0; i < backup.length; i++) {
                cells[i].setValue(backup[i]);
            }
        }
    }

    private void loadPuzzleData(String puzzleJson) {

        try {


            JSONArray array = new JSONArray(puzzleJson);
            if (array.length() > cells.length) return;

            for (int i = 0; i < array.length(); i++) {
                cells[i].loadJSON(array.getString(i));
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void loadPuzzleData(int[] puzzle) {
        if (puzzle.length > cells.length) return;

        for (int i = 0; i < puzzle.length; i++) {
            cells[i].setValue(puzzle[i], true);

            if (puzzle[i] != -1)
                cells[i].lock(true);
        }
    }

    private void softClear() {
        for (Cell cell : cells) cell.setValue(-1);
        gridView.invalidate();
    }

    private void hardClear() {
        for (Cell cell : cells) cell.setValue(-1, true);
        gridView.invalidate();
    }

    private void initializeCells() {

        cells = new Cell[GridSpecs.ROWS * GridSpecs.COLS];
        rows = new Cell[GridSpecs.ROWS][GridSpecs.COLS];
        cols = new Cell[GridSpecs.ROWS][GridSpecs.COLS];
        blks = new Cell[GridSpecs.ROWS][GridSpecs.COLS];


        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {
                Cell cell = new Cell(x, y);

                cells[(y * GridSpecs.COLS) + x] = cell;
                rows[y][x] = cell;
                cols[x][y] = cell;
                int blkY = ((y / (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x / (GridSpecs.COLS / 3));
                int blkX = ((y % (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x % (GridSpecs.COLS / 3));
                blks[blkY][blkX] = cell;
            }
        }
    }

    private void setCellValueFromKeyboard(Cell cell, int value) {
        if (cell == null) return;

        //TODO Add puzzle solve notification

        if (VALUE_SET) {
            cell.setValue(value);

            if (updatePossibilities && cell.getValue() == value) {
                for (Cell c : rows[cell.getRow()]) c.removePossibility(value);
                for (Cell c : cols[cell.getCol()]) c.removePossibility(value);
                for (Cell c : blks[cell.getBlk()]) c.removePossibility(value);
            }

        } else {
            cell.setPosibility(value);
        }

        gridView.invalidate();
    }

    public void updateCellPossibilities(Cell checkCell) {

        if (checkCell.getValue() == -1) {

            // list of all digit possibilities
            ArrayList<Integer> digits = getDigits();

            for (Cell cell : rows[checkCell.getRow()]) {
                if (cell.getValue() != -1) digits.remove(Integer.valueOf(cell.getValue()));
            }
            for (Cell cell : cols[checkCell.getCol()]) {
                if (cell.getValue() != -1) digits.remove(Integer.valueOf(cell.getValue()));
            }
            for (Cell cell : blks[checkCell.getBlk()]) {
                if (cell.getValue() != -1) digits.remove(Integer.valueOf(cell.getValue()));
            }

            checkCell.setPossibilities(digits);

//            gridView.invalidate();

        }
    }

    private ArrayList<Integer> getDigits() {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 9; list.add(i++)) ;
        return list;
    }

    public void hardKeyBoardClick(View v) {

        Cell selected = null;
        SudokuGridView.Coord selectedCoord = gridView.getSelectedCellCoords();

        if (selectedCoord.getX() != -1 && selectedCoord.getY() != -1)
            selected = rows[selectedCoord.getY()][selectedCoord.getX()];

        switch (v.getId()) {

            case (R.id.btn1):
                setCellValueFromKeyboard(selected, 1);
                break;
            case (R.id.btn2):
                setCellValueFromKeyboard(selected, 2);
                break;
            case (R.id.btn3):
                setCellValueFromKeyboard(selected, 3);
                break;
            case (R.id.btn4):
                setCellValueFromKeyboard(selected, 4);
                break;
            case (R.id.btn5):
                setCellValueFromKeyboard(selected, 5);
                break;
            case (R.id.btn6):
                setCellValueFromKeyboard(selected, 6);
                break;
            case (R.id.btn7):
                setCellValueFromKeyboard(selected, 7);
                break;
            case (R.id.btn8):
                setCellValueFromKeyboard(selected, 8);
                break;
            case (R.id.btn9):
                setCellValueFromKeyboard(selected, 9);
                break;
            case (R.id.btnClear):
                if (selected != null) {
                    selected.setValue(-1);
                    gridView.invalidate();
                }
                break;
            case (R.id.btnModeToggle):
                toggleMode();
                break;
//            case (R.id.btnSolve):
//                attemptSolve();
//                break;
        }
    }

    private void toggleMode() {
        VALUE_SET = !VALUE_SET;

        int color = (VALUE_SET) ? getResources().getColor(R.color.gridValueTextColor) : getResources().getColor(R.color.gridPossibilityTextColor);

        try {
            ((Button) findViewById(R.id.btn1)).setTextColor(color);
            ((Button) findViewById(R.id.btn2)).setTextColor(color);
            ((Button) findViewById(R.id.btn3)).setTextColor(color);
            ((Button) findViewById(R.id.btn4)).setTextColor(color);
            ((Button) findViewById(R.id.btn5)).setTextColor(color);
            ((Button) findViewById(R.id.btn6)).setTextColor(color);
            ((Button) findViewById(R.id.btn7)).setTextColor(color);
            ((Button) findViewById(R.id.btn8)).setTextColor(color);
            ((Button) findViewById(R.id.btn9)).setTextColor(color);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    private int attemptSolve() {

        int solved = 0;

        for (Cell cell : cells)
            if (cell.getValue() == -1) {
                updateCellPossibilities(cell);
            } else {
                solved++;
            }

        boolean updated = true;
        while (updated) {

            updated = false;

            for (Cell cell : cells) {

                if (cell.getValue() == -1) {

                    if (singlePossibility(cell)) {
                        updated = true;
                        solved++;

                    } else if (singlePossibilityRecurrence(cell, rows[cell.getRow()])) {
                        updated = true;
                        solved++;

                    } else if (singlePossibilityRecurrence(cell, cols[cell.getCol()])) {
                        updated = true;
                        solved++;

                    } else if (singlePossibilityRecurrence(cell, blks[cell.getBlk()])) {
                        updated = true;
                        solved++;
                    }

                    if (cell.getValue() == -1 && cell.getPossibilities().size() == 0) {
                        Log.i("SUDOKU", "Attempted solve resulted in IMPOSSIBLE at " + cell.getIndex() + " with possibilities " + cell.getPossibilities().toString());
                        return IMPOSSIBLE;
                    }
                }


            }
        }

        if (solved == 81) {
//            Toast.makeText(this, "Solved the puzzle!", Toast.LENGTH_LONG).show();
            Log.i("SUDOKU", "Attempted solve resulted in SOLVED");
            return SOLVED;
        } else {
//            Toast.makeText(this, "Couldn't figure this one out", Toast.LENGTH_LONG).show();
            Log.i("SUDOKU", "Attempted solve resulted in UNSOLVED");
            return UNSOLVED;
        }

    }

    private void doSolve() {

        if (!isValid()) {
            new AlertDialog.Builder(this)
                    .setTitle("Could not solve")
                    .setMessage("There is an error in this puzzle, please fix and try to solve again")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Solving");
        pd.setMessage("Solving the puzzle");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();

        final Runnable completed = new Runnable() {
            @Override
            public void run() {
                gridView.invalidate();
                pd.dismiss();
            }
        };

        Thread solveThread = new Thread(new Runnable() {
            @Override
            public void run() {

                savePuzzleState();

                int result = attemptSolve();
                if (result == SOLVED || result == IMPOSSIBLE) {
                    gridView.post(completed);
                    return;
                }

                ArrayList<Branch> stack = new ArrayList<>();

                for (Cell cell : cells) {
                    if (cell.getValue() == -1) {

                        // add first empty cell to the stack
                        stack.add(new Branch(cell.getIndex(), cell.getPossibilities()));
                        break;

                    }
                }

                while (!stack.isEmpty()) {


                    for (Branch u : stack) {

                        // set values from stack
                        Cell cell = cells[u.n];

                        if (u.first() != -1)
                            cell.setValue(u.first());

//                if(cell.getValue() != u.first() && u.first() != -1) {
//                    cell.setValue(u.first());
//
////                    for (Cell c : rows[cell.getRow()]) c.removePossibility(cell.getValue());
////                    for (Cell c : cols[cell.getCol()]) c.removePossibility(cell.getValue());
////                    for (Cell c : blks[cell.getBlk()]) c.removePossibility(cell.getValue());
//                }
                    }

                    Log.i("SUDOKU", "Trying branch at " + stack.get(stack.size() - 1).n + " with " + stack.get(stack.size() - 1).first());

                    result = attemptSolve();

                    if (result == SOLVED) {
                        gridView.post(completed);
                        return;

                    } else if (result == UNSOLVED) {
                        for (Cell cell : cells) {
                            if (cell.getValue() == -1) {
                                stack.add(new Branch(cell.getIndex(), cell.getPossibilities()));
                                Log.i("SUDOKU", "Creating branch at " + cell.getIndex() + " for " + cell.getPossibilities().toString());
                                break;
                            }
                        }

                    } else if (result == IMPOSSIBLE) {

                        while (stack.size() != 0 && stack.get(stack.size() - 1).list.size() <= 1) {
                            stack.remove(stack.size() - 1);
                            Log.i("SUDOKU", "Backtracking...");

                        }

                        if (stack.size() == 0) {
                            Log.i("SUDOKU", "Queue empty, this puzzle is impossible");
                            gridView.post(completed);
                            return;
                        }

                        stack.get(stack.size() - 1).removeFirst();
                        restorePuzzleState();
                    }


                }

            }
        });

        solveThread.start();
    }

    private boolean singlePossibilityRecurrence(Cell cell, Cell[] cells) {
        if (cell.getValue() != -1) return false;

        ArrayList<Integer> currentPossibilities = new ArrayList<>();
        currentPossibilities.addAll(cell.getPossibilities());

        for (Cell c : cells) {
            if (c.getValue() == -1 && c != cell) {
                currentPossibilities.removeAll(c.getPossibilities());
            }
        }

        if (currentPossibilities.size() == 1) {
            cell.setValue(currentPossibilities.get(0));

            for (Cell c : rows[cell.getRow()]) c.removePossibility(currentPossibilities.get(0));
            for (Cell c : cols[cell.getCol()]) c.removePossibility(currentPossibilities.get(0));
            for (Cell c : blks[cell.getBlk()]) c.removePossibility(currentPossibilities.get(0));

//            gridView.invalidate();
            return true;
        }

        return false;

    }

    private boolean singlePossibility(Cell cell) {
        if (cell.getValue() != -1) return false;

        if (cell.getPossibilities().size() == 1) {

            cell.setValue(cell.getPossibilities().get(0));

            for (Cell c : rows[cell.getRow()]) c.removePossibility(cell.getValue());
            for (Cell c : cols[cell.getCol()]) c.removePossibility(cell.getValue());
            for (Cell c : blks[cell.getBlk()]) c.removePossibility(cell.getValue());

            return true;

        } else {
            return false;
        }
    }

    private boolean isValid() {

        for (Cell[] row : rows) if (isDuplicated(row)) return false;
        for (Cell[] col : cols) if (isDuplicated(col)) return false;
        for (Cell[] blk : blks) if (isDuplicated(blk)) return false;

        return true;

    }

    private boolean isDuplicated(Cell[] cells) {
        ArrayList<Integer> check = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.getValue() != -1 && check.contains(cell.getValue())) {
                return true;
            } else if (cell.getValue() != -1) {
                check.add(cell.getValue());
            }
        }
        return false;
    }

    private class Branch {
        int n;
        ArrayList<Integer> list;

        Branch(int n, ArrayList<Integer> list) {
            this.n = n;
            this.list = list;
        }

        public int first() {
            return (list.size() > 0) ? list.get(0) : -1;
        }

        public void removeFirst() {
            if (list.size() > 0) list.remove(0);
        }
    }
}
