package com.danbuntu.sudokuisfun;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.danbuntu.sudokuisfun.ocr.OCRData;
import com.danbuntu.sudokuisfun.puzzle.Puzzle;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import junit.framework.AssertionFailedError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    /**
     * Test the features of the app
     */
    public ApplicationTest() {
        super(Application.class);
    }

    public void testRecognizesTestImage() {

        Context context = getContext();
        File tempFile = null;
        try {
            tempFile = getTestAssetFile(context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(tempFile);

        final OCRData ocrData = new OCRData(context, tempFile.getAbsolutePath());

        ocrData.beginScan();
        int[] correctArray = new int[]{

               -1, 9, 4,  1, 7, 2,  5, 8,-1,
                1,-1, 7,  3, 8, 6,  2,-1, 9,
                2, 8,-1,  9, 4, 5, -1, 1, 3,

                5, 3, 8, -1, 9,-1,  6, 2, 1,
                9, 4, 1,  2,-1, 3,  8, 7, 5,
                7, 6, 2, -1, 5,-1,  3, 9, 4,

                4, 1,-1,  5, 2, 8, -1, 6, 7,
                6,-1, 9,  4, 3, 7,  1,-1, 8,
               -1, 7, 5,  6, 1, 9,  4, 3,-1

        };

        int correct = 0;
        int[] grid = ocrData.getGridData();
        for (int i = 0; i < correctArray.length; i++) {
            try {
                assertEquals(correctArray[i], grid[i]);
                correct++;
                Log.i("OCR-Test", String.format("Compared index [%s] expect: [%s] got: [%s]", i, correctArray[i], grid[i]));
            } catch (AssertionFailedError e) {
                Log.e("OCR-Test", String.format("Compared index [%s] expect: [%s] got: [%s]", i, correctArray[i], grid[i]));
            }
        }

        assertEquals(String.format("%s correct out of %s", correct, 81), 81, correct);
    }

    public void testBruteForceCompletion() {

        final Puzzle testPuzzle = new Puzzle();

        testPuzzle.setPuzzleListener(new Puzzle.PuzzleListener() {
            @Override
            public void puzzleSolveStarted() {}

            @Override
            public void puzzleSolveEnded() {}

            @Override
            public void puzzleSolved() {
                assertTrue(testPuzzle.isSolved());
                Log.i("TestBruteForce", "Successfully solved the puzzle");
            }

            @Override
            public void puzzleResumed() {}

            @Override
            public void puzzleImpossible() {
                Log.e("TestBruteForce", "Did not solve the puzzle, resulted in IMPOSSIBLE status");
                throw new Error();
            }

            @Override
            public void puzzleError() {
                Log.e("TestBruteForce", "Did not solve the puzzle, resulted in ERROR status");
                throw new Error();
            }

            @Override
            public void undoNotAvailable() {}

            @Override
            public void undoAvailable() {}
        });

        testPuzzle.doSolve();
    }

    public void testPuzzleErrorDetection() {
        final Puzzle testPuzzle = new Puzzle();

        testPuzzle.setPuzzleListener(new Puzzle.PuzzleListener() {
            @Override
            public void puzzleSolveStarted() {}

            @Override
            public void puzzleSolveEnded() {
                assertTrue(!testPuzzle.isSolved());
            }

            @Override
            public void puzzleSolved() {
                Log.e("TestErrorDetection", "Did not detect the error, the puzzle resulted in solved");
                throw new Error();
            }

            @Override
            public void puzzleResumed() {}

            @Override
            public void puzzleImpossible() {}

            @Override
            public void puzzleError() {
                Log.i("TestErrorDetection", "Successfully detected the error");
            }

            @Override
            public void undoNotAvailable() {}

            @Override
            public void undoAvailable() {}
        });

        testPuzzle.setCell(0, 0, 1, false, false);
        testPuzzle.setCell(0, 1, 1, false, false);

        testPuzzle.doSolve();
    }

    private File getTestAssetFile(Context context) throws IOException {
        File tempFile = SudokuUtils.createTemporaryFile(context);

        InputStream is = context.getAssets().open("ocr-test.png");
        FileOutputStream fos = new FileOutputStream(tempFile);

        int read;
        byte[] buffer = new byte[1024];
        while ((read = is.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }

        is.close();
        fos.close();

        return tempFile;
    }
}