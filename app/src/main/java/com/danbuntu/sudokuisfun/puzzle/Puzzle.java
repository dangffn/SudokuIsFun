package com.danbuntu.sudokuisfun.puzzle;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.danbuntu.sudokuisfun.ui.SudokuGridView;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Dan on 6/22/2016. Have a great day!
 */
public class Puzzle {

    final static int SOLVED = 0;
    final static int IMPOSSIBLE = -3;
    final static int UNSOLVED = -2;
    final static int RESUMED = 1;
    final static int UNDO_AVAILABLE = 2;
    final static int UNDO_NOT_AVAILABLE = 3;

    PuzzleListener listener;

    Cell[][] mRows, mCols, mBlks;
    Cell[] mCells;
    int[] mSoftBackup, mHardBackup;
    int mPosReductions = 0, mValReductions = 0, mBottlenecks = 0;
    int mSolveScans = 0;
    int mGuesses;
    int hintsShowing = 0;
    short currentRevision = 0;
    boolean logMode, showConflicts, computerAssist = false;
    SudokuGridView gridView;
    int mSolvedCells = 0;
    boolean solved = false;
    AtomicBoolean isSolving;
    Handler handler;
    Thread solveThread;

    /**
     * Use this constructor if you don't want the puzzle to be attached to a SudokuGridView
     * ex. If you want to run tests on a Sudoku puzzle in the background without the gui
     */
    public Puzzle() {
        this(null);
    }

    /**
     * @param gridView a SudokuGridView that the puzzle will update when changes are made
     */
    public Puzzle(SudokuGridView gridView) {
        initializeCells();
        isSolving = new AtomicBoolean(false);
        this.gridView = gridView;
        if (gridView != null) this.gridView.bindToCells(mRows);
        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (listener == null) return;

                switch (msg.what) {
                    case (SOLVED):
                        if (isSolving.get()) return;
                        listener.puzzleSolved();
                        listener.puzzleSolveEnded();
                        mSoftBackup = null;
                        mHardBackup = null;
                        if (mGuesses > -1)
                            //Log.i("Puzzle", "Puzzle was solved with " + mGuesses + " guesses made");
                            break;
                    case (IMPOSSIBLE):
                        listener.puzzleImpossible();
                        listener.puzzleSolveEnded();
                        mSoftBackup = null;
                        mHardBackup = null;
                        break;
                    case (UNSOLVED):
                        listener.puzzleImpossible();
                        listener.puzzleSolveEnded();
                        break;
                    case (RESUMED):
                        listener.puzzleResumed();
                        break;
                    case (UNDO_AVAILABLE):
                        listener.undoAvailable();
                        break;
                    case (UNDO_NOT_AVAILABLE):
                        listener.undoNotAvailable();
                        break;
                }
            }
        };
    }

    public static void substituteCellValuesRandomly(Cell[][] array) {
        Integer[] digits = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        List<Integer> random = Arrays.asList(digits);
        Collections.shuffle(random);

        for (int y = 0; y < array.length; y++) {
            for (int x = 0; x < array[0].length; x++) {
                int index = array[y][x].getValue() % random.size();
                if (array[y][x].getValue() != Cell.NONE) {
                    boolean locked = array[y][x].isLocked();
                    array[y][x].setValue(random.get(index), true);
                    array[y][x].setLocked(locked);
                } else {
                    ArrayList<Integer> substitutedPossibilities = new ArrayList<>();
                    for (int i : array[y][x].getPossibilities())
                        substitutedPossibilities.add(random.get(i % random.size()));
                    array[y][x].clear();
                    array[y][x].setPossibilities(substitutedPossibilities);
                }
            }
        }
    }

    /**
     * @param set a boolean to determine whether or not the puzzle should record Cell history when changes are made to them
     */
    public void setLogMode(boolean set) {
        logMode = set;
    }

    public boolean inLogMode() {
        return logMode;
    }

    private void initializeCells() {

        mCells = new Cell[GridSpecs.ROWS * GridSpecs.COLS];
        mRows = new Cell[GridSpecs.ROWS][GridSpecs.COLS];
        mCols = new Cell[GridSpecs.ROWS][GridSpecs.COLS];
        mBlks = new Cell[GridSpecs.ROWS][GridSpecs.COLS];


        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {
                Cell cell = new Cell(x, y, this);

                mCells[(y * GridSpecs.COLS) + x] = cell;
                mRows[y][x] = cell;
                mCols[x][y] = cell;
                int blkY = ((y / (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x / (GridSpecs.COLS / 3));
                int blkX = ((y % (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x % (GridSpecs.COLS / 3));
                mBlks[blkY][blkX] = cell;
            }
        }
    }

    /**
     * Clears the Cells in the Sudoku puzzle
     *
     * @param hardClear if true, force the Cells to clear (ex. if they are locked)
     */
    public void clearPuzzle(boolean hardClear) {

        if (hintsShowing != 0) clearHints();

        if (hardClear) {
            for (Cell cell : mCells) cell.clear(true);
        } else {
            boolean updated = false;
            for (Cell cell : mCells) {
                if (cell.clear()) updated = true;
            }
            if (updated) incrementRev();
        }

        isValid(showConflicts);

    }

    /**
     * Update all of the available possibilities for each cell
     */
    public void displayAllPossibilities() {
        if (updateAllCellPossibilities()) {
            incrementRev();
            computerAssist = true;
        }
    }

    private boolean updateAllCellPossibilities() {
        boolean updated = false;
        for (Cell cell : mCells) {
            if (cell.notSet() && updateCellPossibilities(cell)) updated = true;
        }
        return updated;
    }

    private void incrementRev() {
        if (listener != null) handler.sendEmptyMessage(UNDO_AVAILABLE);
        // on the off chance that the user makes 32767 moves, don't crash and burn
        if (currentRevision < Short.MAX_VALUE) currentRevision++;
    }

    private void setRevisionNumber(short rev) {
        if (rev >= 0) {
            currentRevision = rev;
        } else {
            currentRevision = 0;
        }
        if (listener != null) handler.sendEmptyMessage(UNDO_NOT_AVAILABLE);
    }

    private boolean updateCellPossibilities(Cell checkCell) {

        if (checkCell.notSet()) {

            // list of all digit possibilities
            ArrayList<Integer> digits = getDigits();

            for (Cell cell : mRows[checkCell.getRow()]) {
                if (!cell.notSet()) digits.remove(Integer.valueOf(cell.getValue()));
            }
            for (Cell cell : mCols[checkCell.getCol()]) {
                if (!cell.notSet()) digits.remove(Integer.valueOf(cell.getValue()));
            }
            for (Cell cell : mBlks[checkCell.getBlk()]) {
                if (!cell.notSet()) digits.remove(Integer.valueOf(cell.getValue()));
            }

            return checkCell.setPossibilities(digits);

        }
        return false;
    }

    public short getCurrentRevision() {
        return (logMode) ? currentRevision : -1;
    }

    private void removePossibility(Cell cell, int value) {
        for (Cell c : mRows[cell.getRow()]) c.removePossibility(value);
        for (Cell c : mCols[cell.getCol()]) c.removePossibility(value);
        for (Cell c : mBlks[cell.getBlk()]) c.removePossibility(value);
    }

    private ArrayList<Integer> getDigits() {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 9; list.add(i++)) ;
        return list;
    }

    private int quickSolve() {

        updateAllCellPossibilities();

        boolean updated = true;
        while (updated && mSolvedCells < GridSpecs.ROWS * GridSpecs.COLS && isSolving.get()) {

            updated = false;
            int value;

            for (Cell cell : mCells) {

                if (cell.notSet()) {

                    mValReductions += (value = singlePossibility(cell));
                    if (value == Cell.NONE)
                        mPosReductions += (value = singlePossibilityRecurrence(cell, mRows[cell.getRow()]));
                    if (value == Cell.NONE)
                        mPosReductions += (value = singlePossibilityRecurrence(cell, mCols[cell.getCol()]));
                    if (value == Cell.NONE)
                        mPosReductions += (value = singlePossibilityRecurrence(cell, mBlks[cell.getBlk()]));

                    if (value != Cell.NONE) {
                        cell.setValue(value);
                        removePossibility(cell, value);
                        updated = true;

                    } else if (cell.getPossibilities().size() == 0) {
                        //Log.i("SUDOKU", "Attempted solve resulted in IMPOSSIBLE at " + cell.getIndex() + " with possibilities " + cell.getPossibilities().toString());
                        return IMPOSSIBLE;
                    }
                }
            }
        }

        if (mSolvedCells == GridSpecs.ROWS * GridSpecs.COLS) {
            //Log.i("SUDOKU", "Attempted solve resulted in SOLVED");
            return SOLVED;
        } else {
            //Log.i("SUDOKU", "Attempted solve resulted in UNSOLVED");
            return UNSOLVED;
        }
    }

    public int analyticSolve() {
        updateAllCellPossibilities();

        HashMap<Integer, Integer> queue = new HashMap<>();
        int n;

        boolean updated = true;
        while (updated && mSolvedCells < GridSpecs.ROWS * GridSpecs.COLS && isSolving.get()) {

            updated = false;

            if ((n = tryValReduction(queue)) > 0) {
                updated = true;
                mValReductions += n;
            }

            if (!updated) {
                while ((n = tryPosReduction(queue)) > 0) {

                    updated = true;
                    mPosReductions += n;

                    n = tryValReduction(queue);
                    mValReductions += n;
                }
            }

            for (int key : queue.keySet()) {
                Cell thisCell = mCells[key];
                int val = queue.get(key);
                if (thisCell.getPossibilities().contains(val)) {
                    thisCell.setValue(val);
                    removePossibility(thisCell, val);
                } else {
                    // an error was made somewhere because the values we guessed were wrong
                    return IMPOSSIBLE;
                }
            }
            if (queue.keySet().size() == 1) mBottlenecks++;
            mSolveScans++;
            queue = new HashMap<>();
        }

        if (mSolvedCells == GridSpecs.ROWS * GridSpecs.COLS) {
            //Log.i("SUDOKU", "Attempted solve resulted in SOLVED");
            return SOLVED;
        } else {
            //Log.i("SUDOKU", "Attempted solve resulted in UNSOLVED");
            for (Cell c : mCells)
                if (c.notSet() && c.getPossibilities().size() == 0) return IMPOSSIBLE;
            return UNSOLVED;
        }
    }

    private int tryValReduction(HashMap<Integer, Integer> updateQueue) {
        int updated = 0;
        int value;

        for (Cell cell : mCells) {
            if (cell.notSet() && !updateQueue.keySet().contains(cell.getIndex())) {
                if ((value = singlePossibility(cell)) != Cell.NONE) {
                    updateQueue.put(cell.getIndex(), value);
                    updated++;
                }
            }
        }
        return updated;
    }

    private int tryPosReduction(HashMap<Integer, Integer> updateQueue) {
        int updated = 0;
        int value;

        for (Cell cell : mCells) {
            int row = cell.getRow();
            int col = cell.getCol();
            int blk = cell.getBlk();

            if (cell.notSet() && !updateQueue.keySet().contains(cell.getIndex())) {
                if ((value = singlePossibilityRecurrence(cell, mRows[row])) != Cell.NONE) {
                    updateQueue.put(cell.getIndex(), value);
                    updated++;
                    return updated;
                } else if ((value = singlePossibilityRecurrence(cell, mCols[col])) != Cell.NONE) {
                    updateQueue.put(cell.getIndex(), value);
                    updated++;
                    return updated;
                } else if ((value = singlePossibilityRecurrence(cell, mBlks[blk])) != Cell.NONE) {
                    updateQueue.put(cell.getIndex(), value);
                    updated++;
                    return updated;
                }
            }
        }
        return updated;
    }

    public boolean didComputerAssist() {
        return computerAssist;
    }

    private ArrayList<Branch> newSolveQueue() {
        ArrayList<Branch> stack = new ArrayList<>();
        for (Cell cell : mCells) {
            if (cell.notSet()) {
                // add first empty cell to the stack
                stack.add(new Branch(cell.getIndex(), cell.getPossibilities()));
                break;
            }
        }
        return stack;
    }

    /**
     * @return The number of guesses that had to be made while solving the puzzle
     * returns -1 if the puzzle has not been solved
     */
    public int getGuessCount() {
        return mGuesses;
    }

    public int getBottleneckCount() {
        return mBottlenecks;
    }

    public int getDifficulty(int min, int max) {
        if (!isSolved() || min > max || min == max) return -1;

        float bW = 0.015f;
        float gW = 0.2f;
        float pW = 0.015f;

        float bR = (float) mBottlenecks * bW;
        float gR = (float) mGuesses * gW;
        float pR = (float) mPosReductions * pW;

        int spectrum = max - min;

        // adjust the value to reflect the requested spectrum of values
        float total = (bR + gR + pR);
        float adjustedTotal = total * spectrum;

        //Log.i("PUZZLE", "The difficulty for this puzzle is: " + total);

        return (int) adjustedTotal + min;
    }

    private void clearUserEdits(int[] backup) {
        int userEdited = 0;
        for (int i = 0; i < backup.length; i++) {
            if (backup[i] != Cell.NONE && !mCells[i].isLocked()) {
                backup[i] = Cell.NONE;
                userEdited++;
            }
        }
        //Log.i("SUDOKU", "Removed " + userEdited + " user edits and restarted the queue");
    }

    public void doSolve() {
        doSolve(false, false);
    }

    public void doSolve(boolean analytic) {
        doSolve(analytic, false);
    }

    public void doSolve(final boolean analytic, boolean waitForFinish) {

        isSolving.set(true);
        computerAssist = true;
        mGuesses = 0;

        if (!isValid(showConflicts)) {
            if (listener != null) listener.puzzleError();
            isSolving.set(false);
            return;
        }

        if (listener != null) listener.puzzleSolveStarted();

        solveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean removedUserInput = false;

                savePuzzleState();

                // pre-scan to find out if we can do this puzzle
                final int preResult = (analytic) ? analyticSolve() : quickSolve();
                if (preResult == SOLVED) {
                    incrementRev();
                    isSolving.set(false);
                    handler.sendEmptyMessage(SOLVED);
                    return;
                } else if (preResult == IMPOSSIBLE) {
                    removedUserInput = true;
                    clearUserEdits(mSoftBackup);
                    restorePuzzleState();
                }

                ArrayList<Branch> stack = newSolveQueue();

                while (!stack.isEmpty() && isSolving.get()) {

                    // this should be the number of guesses that were made when the puzzle was solved
                    // not the max number of guesses that were made while attempting to solve
                    mGuesses = stack.size();

                    for (Branch u : stack) {

                        // set values from stack
                        Cell cell = mCells[u.n];

                        if (u.first() != -1)
                            cell.setValue(u.first());

                    }

                    //Log.i("SUDOKU", "Trying branch at " + stack.get(stack.size() - 1).n + " with " + stack.get(stack.size() - 1).first());

                    int result = (analytic) ? analyticSolve() : quickSolve();

                    if (result == SOLVED) {

                        if (!Arrays.equals(mSoftBackup, mHardBackup)) {
                            for (int i = 0; i < mHardBackup.length; i++) {
                                if (mHardBackup[i] != Cell.NONE && mHardBackup[i] != mCells[i].getValue()) {
                                    mCells[i].setHint(true);
                                    hintsShowing++;
                                }
                            }
                        }

                        incrementRev();
                        isSolving.set(false);
                        handler.sendEmptyMessage(SOLVED);
                        return;

                    } else if (result == UNSOLVED) {
                        for (Cell cell : mCells) {
                            if (cell.notSet()) {
                                Branch newBranch = new Branch(cell.getIndex(), cell.getPossibilities());
                                newBranch.setPosCount(mPosReductions);
                                newBranch.setValCount(mValReductions);
                                newBranch.setBotCount(mBottlenecks);
                                newBranch.setScaCount(mSolveScans);
                                stack.add(newBranch);
                                //Log.i("SUDOKU", "Creating branch at " + cell.getIndex() + " for " + cell.getPossibilities().toString());
                                break;
                            }
                        }

                    } else if (result == IMPOSSIBLE) {

                        while (stack.size() != 0 && stack.get(stack.size() - 1).list.size() <= 1) {
                            if (!isSolving.get())
                                return;

                            if (stack.size() > 0) {
                                Branch lastBranch = stack.remove(stack.size() - 1);
                                mPosReductions = lastBranch.posCount;
                                mValReductions = lastBranch.valCount;
                                mBottlenecks = lastBranch.botCount;
                                mSolveScans = lastBranch.scaCount;
                            } else {
                                mPosReductions = 0;
                                mValReductions = 0;
                                mBottlenecks = 0;
                                mSolveScans = 0;
                            }
                            //Log.i("SUDOKU", "Backtracking...");

                        }

                        if (stack.size() <= 0) {

                            // queue is empty, remove any user changes and retry solving the base puzzle
                            if (!removedUserInput) {
                                removedUserInput = true;
                                clearUserEdits(mSoftBackup);
                                restorePuzzleState();
                                stack = newSolveQueue();
                                continue;
                            }

                            // stack is empty and user changes were already removed, this puzzle is completely impossible
                            //Log.i("SUDOKU", "Queue empty, this puzzle is impossible");
                            handler.sendEmptyMessage(IMPOSSIBLE);
                            incrementRev();
                            isSolving.set(false);
                            return;

                        } else {
                            Branch lastBranch = stack.get(stack.size() - 1);
                            mPosReductions = lastBranch.posCount;
                            mValReductions = lastBranch.valCount;
                            mBottlenecks = lastBranch.botCount;
                            mSolveScans = lastBranch.scaCount;
                            lastBranch.removeFirst();
                        }

                        restorePuzzleState();
                    }


                }

            }
        });

        solveThread.start();

        if (waitForFinish) {
            try {
                solveThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                isSolving.set(false);
            }
        }
    }

    public boolean isSolving() {
        return isSolving.get();
    }

    public void cancelSolving() {
        if (solveThread != null) {
            try {
                isSolving.set(false);
                solveThread.interrupt();
                solveThread.join();

            } catch(InterruptedException e) {
                e.printStackTrace();
            } finally {
                solveThread = null;
            }
        }
    }

    private int singlePossibilityRecurrence(Cell cell, Cell[] cells) {
        if (!cell.notSet()) return cell.getValue();

        int value = Cell.NONE;

        ArrayList<Integer> currentPossibilities = new ArrayList<>();
        currentPossibilities.addAll(cell.getPossibilities());

        for (Cell c : cells) {
            if (c.notSet() && c != cell) {
                currentPossibilities.removeAll(c.getPossibilities());
            }
        }

        if (currentPossibilities.size() == 1) value = currentPossibilities.get(0);

        return value;
    }

    private int singlePossibility(Cell cell) {
        int value = Cell.NONE;

        if (cell.notSet()) {
            if (cell.getPossibilities().size() == 1) value = cell.getPossibilities().get(0);
        } else {
            value = cell.getValue();
        }
        return value;
    }

    private void checkIndividualCellValidity(Cell cell, boolean markErrors) {
        // after a cell is updated, call this method to set alert status on all of the conflicting neighbor cells
        if (cell == null) return;

        // avoids checking the same cell twice and eating up more cpu
        ArrayList<Cell> checked = new ArrayList<>();

        Queue<Cell> queue = new LinkedList<>();
        queue.add(cell);

        while (!queue.isEmpty()) {
            Cell checkCell = queue.remove();
            checkCellNeighborValidity(checkCell, queue, checked, markErrors);
        }
    }

    private boolean checkCellNeighborValidity(Cell cell, Queue<Cell> queue, ArrayList<Cell> checked, boolean markErrors) {
        // checks the cell neighbors to see if there are any conflicting values
        if (checked.contains(cell)) return true;
        checked.add(cell);

        boolean valid = true;
        if (!queueSegmentCheck(cell, mRows[cell.getRow()], queue, markErrors)) valid = false;
        if (!queueSegmentCheck(cell, mCols[cell.getCol()], queue, markErrors)) valid = false;
        if (!queueSegmentCheck(cell, mBlks[cell.getBlk()], queue, markErrors)) valid = false;

        if ((markErrors && showConflicts) || (gridView != null && gridView.inEditMode())) {
            cell.setAlert(!valid);
        }
        return valid;
    }

    private boolean queueSegmentCheck(Cell cell, Cell[] cells, Queue<Cell> queue, boolean markErrors) {
        boolean valid = true;
        for (Cell c : cells) {
            int val = c.getValue();
            if (c != cell && val != Cell.NONE) {
                if (val == cell.getValue()) {

                    // this cell has the same value as the
                    if ((markErrors && showConflicts) || (gridView != null && gridView.inEditMode())) {
                        cell.setAlert(true);
                        c.setAlert(true);
                    }
                    valid = false;
                } else if (c.isAlert()) {
                    queue.add(c);
                }
            }
        }
        return valid;
    }

    /**
     * @param markErrors whether or not to show errors that were found in the puzzle
     * @return returns true if there were no errors, false if there were errors
     */
    public boolean isValid(boolean markErrors) {

        boolean valid = true;

        ArrayList<Cell> checked = new ArrayList<>();
        Queue<Cell> queue = new LinkedList<>();

        for (Cell cell : mCells) {
            if (cell.getValue() != Cell.NONE) queue.add(cell);
        }
        while (!queue.isEmpty()) {
            Cell checkCell = queue.remove();
            if (!checkCellNeighborValidity(checkCell, queue, checked, markErrors)) valid = false;
        }

        //Log.i("PUZZLE", "Puzzle valid: " + valid);

        return valid;

    }

    private void savePuzzleState() {
        mSoftBackup = new int[mCells.length];
        mHardBackup = new int[mCells.length];

        for (int i = 0; i < mCells.length; i++) {
            mSoftBackup[i] = mCells[i].getValue();
            mHardBackup[i] = mCells[i].getValue();
        }
    }

    private void restorePuzzleState() {
        if (mSoftBackup != null) {
            for (int i = 0; i < mSoftBackup.length; i++) {
                if (mSoftBackup[i] == Cell.NONE) {
                    mCells[i].clear();
                } else {
                    mCells[i].setValue(mSoftBackup[i]);
                }
            }
        }
        mPosReductions = 0;
        mValReductions = 0;
        mBottlenecks = 0;
        mSolveScans = 0;
    }

    public void loadPuzzleData(String puzzleJson) {
        loadPuzzleData(puzzleJson, (short) 0);
    }

    public void loadPuzzleData(String puzzleJson, short rev) {
        if (puzzleJson == null) return;

        try {
            JSONArray array = new JSONArray(puzzleJson);
            if (array.length() > mCells.length) return;

            for (int i = 0; i < array.length(); i++) {
                mCells[i].loadJSON(array.getString(i));
            }

            setRevisionNumber(rev);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void loadPuzzleData(int[] puzzle) {
        if (puzzle == null) return;

        // suspend logMode so we don't record history when loading the puzzle
        boolean log = inLogMode();
        setLogMode(false);

        if (puzzle.length > mCells.length) return;

        for (int i = 0; i < puzzle.length; i++) {
            mCells[i].setValue(puzzle[i], true);

            if (puzzle[i] != -1) mCells[i].setLocked(true);
        }

        setLogMode(log);
    }

    public void setComputerAssist(boolean set) {
        computerAssist = set;
    }

    public Cell[] getCellArray() {
        return mCells;
    }

    public int[] getPuzzleData() {
        int[] out = new int[mCells.length];
        for (int i = 0; i < mCells.length; i++) out[i] = mCells[i].getValue();
        return out;
    }

    public Bundle onSaveInstanceState() {

        Bundle bundle = new Bundle();
        JSONArray array = new JSONArray();

        try {
            for (Cell cell : mCells) {
                array.put(cell.toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        bundle.putString("CELLS", array.toString());
        bundle.putShort("REVISION", currentRevision);
        bundle.putBoolean("COMPUTER_ASSIST", computerAssist);
        return bundle;
    }

    public void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            loadPuzzleData(savedState.getString("CELLS"), savedState.getShort("REVISION"));
            computerAssist = savedState.getBoolean("COMPUTER_ASSIST");
        }
    }


    /**
     *
     */
    public void onCellValueSet() {
        if (mSolvedCells == 81) {
            if (isValid(showConflicts)) {
                solved = true;
                handler.sendEmptyMessage(SOLVED);
            } else {
                solved = false;
                handler.sendEmptyMessage(RESUMED);
            }
        }
//        //Log.i("PUZZLE", "Current solved cells: " + mSolvedCells);
    }

    public void onCellValueSet(boolean set) {
        if (set) {
            mSolvedCells++;
            if (mSolvedCells == 81 && isValid(showConflicts)) {
                solved = true;
                handler.sendEmptyMessage(SOLVED);
            }
        } else {
            if (mSolvedCells == 81) {
                solved = false;
                handler.sendEmptyMessage(RESUMED);
            }
            mSolvedCells--;
        }
//        //Log.i("PUZZLE", "Current solved cells: " + mSolvedCells);
    }

    public int getSolvedCount() {
        return mSolvedCells;
    }

    public boolean isSolved() {
        return solved;
    }

    private void clearHints() {
        for (Cell cell : mCells) cell.setHint(false);
        hintsShowing = 0;
    }

    public void setCell(int x, int y, int value, boolean editMode, boolean autoUpdatePossibilities) {
        if (x < 0 || y < 0 || x > mCols.length || y > mRows.length) return;

        if (hintsShowing != 0) clearHints();

        if (!editMode) {

            // log this change
            if (mRows[y][x].setValue(value)) {
                if (autoUpdatePossibilities) removePossibility(mRows[y][x], value);
                incrementRev();
                checkIndividualCellValidity(mRows[y][x], showConflicts);
            }

        } else {

            mRows[y][x].setValue(value, true);
            if (value != -1) mRows[y][x].setLocked(true);
            // always show conflicts when editing a cell
            checkIndividualCellValidity(mRows[y][x], true);

        }
    }

    public boolean obfuscatePuzzle() {
        Object[][] obfuscatedPuzzle = new JSONObject[mRows.length][mCols.length];
        try {
            for (int y = 0; y < mRows.length; y++) {
                for (int x = 0; x < mCols.length; x++) {
                    obfuscatedPuzzle[y][x] = mRows[y][x].toJSON();
                }
            }

            obfuscatedPuzzle = SudokuUtils.shuffleArray(obfuscatedPuzzle);
            obfuscatedPuzzle = SudokuUtils.rotateArray(obfuscatedPuzzle, true);
            obfuscatedPuzzle = SudokuUtils.shuffleArray(obfuscatedPuzzle);

            for (int y = 0; y < mRows.length; y++) {
                for (int x = 0; x < mCols.length; x++) {
                    // restore the json but dont override the history
                    mRows[y][x].loadJSON((JSONObject) obfuscatedPuzzle[y][x], false);
                }
            }

            substituteCellValuesRandomly(mRows);

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        if (inLogMode()) incrementRev();
        return true;
    }

    public void togglePossibility(int x, int y, int possibility) {
        if (x < 0 || y < 0 || x > mCols.length || y > mRows.length) return;

        if (hintsShowing != 0) clearHints();

        if (mRows[y][x].getPossibilities().contains(possibility)) {

            if (mRows[y][x].removePossibility(possibility)) incrementRev();

        } else {

            if (mRows[y][x].addPossibility(possibility)) incrementRev();

        }

    }

    public void clearCell(int x, int y, boolean force) {
        if (x < 0 || y < 0 || x > mCols.length || y > mRows.length) return;

        if (hintsShowing != 0) clearHints();

        if (mRows[y][x].clear(force)) {
            checkIndividualCellValidity(mRows[y][x], showConflicts);
            incrementRev();
        }

    }

    public boolean isFinished() {
        return mSolvedCells == 81;
    }

    public int getHintCount() {
        return hintsShowing;
    }

    public void showConflicts(boolean set) {
        showConflicts = set;

        if (!showConflicts && gridView != null && !gridView.inEditMode()) {
            for (Cell c : mCells) c.setAlert(false);
        } else if (showConflicts) {
            isValid(true);
        }
    }

    public void revert() {

        if (hintsShowing != 0) clearHints();

        if (currentRevision <= 0) {
            if (listener != null) handler.sendEmptyMessage(UNDO_NOT_AVAILABLE);
            return;
        }

        currentRevision--;
        if (currentRevision == 0 && listener != null) handler.sendEmptyMessage(UNDO_NOT_AVAILABLE);

        Cell lastUpdated = null;
        ArrayList<Cell> toScan = new ArrayList<>();
        for (Cell cell : mCells) {

            // if the value of a cell was reverted, queue it up for a validity check
            // don't worry about possibility updates because those wont change cell alert statuses
            int update = cell.revertToStatus(currentRevision);

            if (update != Cell.NONE) {

                if (update == Cell.VAL_CHANGE) toScan.add(cell);
                lastUpdated = cell;

            }
        }

        if (toScan.size() == 1) {
            lastUpdated = toScan.get(0);
        } else if (toScan.size() > 1) {
            // in this case, multiple cells were updated simultaneously and we dont know which one was selected
            // most likely because the user solved the puzzle, or cleared it, so just keep the selection where it is
            lastUpdated = null;
        }

        if (lastUpdated != null) {
            // reposition the highlighted cell based on what was reverted
            if (gridView != null)
                gridView.setSelectedCellCoords(lastUpdated.getCol(), lastUpdated.getRow());
        }

        if (showConflicts) {
            for (Cell cell : toScan) {
                // these cells' values were just reverted, scan them to find conflicts
                //Log.i("Revert", "Checking cell: " + cell.getIndex() + " showConflicts: " + showConflicts);
                checkIndividualCellValidity(cell, showConflicts);
            }
        }

        //Log.i("Puzzle", "Reverted to currentRevision: " + currentRevision);
    }

    public void setPuzzleListener(PuzzleListener listener) {
        this.listener = listener;
    }

    public void stopSolving() {
        isSolving.set(false);
    }

    public interface PuzzleListener {
        void puzzleSolveStarted();

        void puzzleSolveEnded();

        void puzzleSolved();

        void puzzleResumed();

        void puzzleImpossible();

        void puzzleError();

        void undoNotAvailable();

        void undoAvailable();
    }

    /**
     * Used to recursively solve a puzzle
     * A branch represents a "guess" that had to be made in order to solve the puzzle
     */
    private class Branch {
        int n;
        int posCount = 0;
        int valCount = 0;
        int botCount = 0;
        int scaCount = 0;
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

        public void setPosCount(int n) {
            posCount = n;
        }

        public void setValCount(int n) {
            valCount = n;
        }

        public void setBotCount(int n) {
            botCount = n;
        }

        public void setScaCount(int n) {
            scaCount = n;
        }
    }
}