package com.danbuntu.sudokuisfun;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.danbuntu.sudokuisfun.puzzle.Puzzle;
import com.danbuntu.sudokuisfun.ui.PuzzleManager;
import com.danbuntu.sudokuisfun.ui.SudokuGridView;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Manipulate parts of the app to prepare for release
 */
public class ManipulationTest extends ApplicationTestCase<Application> {

    Puzzle puzzle;
    SudokuGridView gridView;

    public ManipulationTest() {
        super(Application.class);
    }

    public void testDatabaseGrab() throws IOException {
        File databaseFile = getContext().getDatabasePath("puzzles.db");

        PuzzleManager pm = PuzzleManager.getInstance(getContext());
        pm.close();

        assertTrue(databaseFile.exists());

        File newFile = new File("/sdcard/puzzles.db");
        if(!newFile.exists()) {
            assertTrue(newFile.createNewFile());
        }

        FileInputStream fis = new FileInputStream(databaseFile);
        FileOutputStream fos = new FileOutputStream(newFile);

        int count;
        byte[] buffer = new byte[1024];
        while((count = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, count);
        }
        fis.close();
        fos.close();
    }

    public void testLoadPuzzlesFromTextFile() throws IOException {

        PuzzleManager pm = PuzzleManager.getInstance(getContext());

        File easyFile = new File("/sdcard/puzzles-easy.txt");
        File mediumFile = new File("/sdcard/puzzles-medium.txt");
        File hardFile = new File("/sdcard/puzzles-hard.txt");

        int loaded;
        loaded = loadFromFile(pm, easyFile, "Easy");
        Log.i("ApplicationTest", "Loaded " + loaded + " easy puzzles");

        loaded = loadFromFile(pm, mediumFile, "Medium");
        Log.i("ApplicationTest", "Loaded " + loaded + " medium puzzles");

        loaded = loadFromFile(pm, hardFile, "Hard");
        Log.i("ApplicationTest", "Loaded " + loaded + " hard puzzles");

        pm.close();

    }

    public void testDuplicateAllPuzzlesRandomly() {

        PuzzleManager pm = PuzzleManager.getInstance(getContext());

        ArrayList<String> all = pm.getAllPuzzleNames("Hard");

        int total = all.size();
        int n = 0;

        while(!all.isEmpty()) {
            String currentPuzzle = all.remove(0);
            int[] data = pm.getPuzzleData(currentPuzzle, false);
            createRandomizedPuzzle(pm, data);
            n++;
            Log.i("Manipulation Test", "Created random puzzle " + n + " of " + total);
        }

        pm.close();
    }



    private int[] puzzleFromString(String input) {
        if(input.length() != 81) {
            Log.e("ManipulationTest", "Attempted to load puzzle from string with length of: " + input.length());
            return null;
        }

        int[] puzzle = new int[input.length()];
        Arrays.fill(puzzle, -1);
        try {
            for (int i = 0; i < input.length(); i++) {
                int n = Character.getNumericValue(input.charAt(i));
                if(n >= 1 || n <= 9) {
                    puzzle[i] = n;
                }
            }
            return puzzle;

        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createRandomizedPuzzle(PuzzleManager pm, int[] puzzleData) {
        String newName = pm.nextAvailableName("Batch Created Random");

        initPuzzle();

        puzzle.loadPuzzleData(puzzleData);
        assertTrue(puzzle.obfuscatePuzzle());
        String category = getContext().getString(R.string.puzzle_category_DEFAULT);

        int size = getContext().getResources().getInteger(R.integer.icon_size);
        pm.saveNewPuzzle(gridView.getSnapshot(size, size, size, false),
            newName, "Batch Created Puzzle", puzzle.getPuzzleData(), category, false);
    }

    private int loadFromFile(PuzzleManager pm, File file, String category) throws IOException {
        int n = 0;
        if(file.exists()) {

            int read;
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                while ((read = fis.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read));
                }
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                if(fis != null) fis.close();
            }

            String[] texts = sb.toString().split("\\r\\n|\\n|\\r");
            for(String text : texts) {
                int[] data = puzzleFromString(text);
                if(data != null) {
                    createPuzzle(pm, data, category);
                    n++;
                }
            }

        }

        return n;
    }

    private void createPuzzle(PuzzleManager pm, int[] data, String category) {
        initPuzzle();

        puzzle.loadPuzzleData(data);
        SudokuUtils.createNewPuzzle(getContext(),
                pm,
                pm.nextAvailableName("Imported Puzzle"),
                data,
                category,
                false,
                true);
    }

    private void initPuzzle() {
        if(gridView == null || puzzle == null) {
            gridView = new SudokuGridView(getContext());
            puzzle = new Puzzle(gridView);
        } else {
            puzzle.clearPuzzle(true);
        }
    }
}