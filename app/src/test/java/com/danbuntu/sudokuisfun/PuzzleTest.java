package com.danbuntu.sudokuisfun;


import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by dan on 7/26/16. Have a great day!
 */
public class PuzzleTest {
    @Test
    public void testRotatesArrayCorrectly() throws Exception {
        Object[][] baseArray = { { 0, 1 }, { 2, 3 } };

        Assert.assertArrayEquals(
                SudokuUtils.rotateArray(baseArray, true),
                new int[][]{ { 2, 0 }, { 3, 1 } }
        );

        Assert.assertArrayEquals(
                SudokuUtils.rotateArray(baseArray, false),
                new int[][]{ { 1, 3 }, { 0, 2 } }
        );
    }
}
