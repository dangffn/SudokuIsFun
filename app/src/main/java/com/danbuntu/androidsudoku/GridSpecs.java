package com.danbuntu.androidsudoku;

/**
 * Created by Dan on 4/21/2016.
 */
public class GridSpecs {

    final float LEFT, TOP, RIGHT, BOTTOM;
    final float GRID_HEIGHT, GRID_WIDTH;
    final float ROW_HEIGHT, COL_WIDTH;
    final static int ROWS = 9;
    final static int COLS = 9;
    final float BLOCK_WIDTH, BLOCK_HEIGHT;

    public GridSpecs(float width, float height) {
        this.GRID_WIDTH = width;
        this.GRID_HEIGHT = height;
        this.ROW_HEIGHT = GRID_HEIGHT / ROWS;
        this.COL_WIDTH = GRID_WIDTH / COLS;
        this.LEFT = 0;
        this.RIGHT = width;
        this.TOP = 0;
        this.BOTTOM = height;
        this.BLOCK_WIDTH = width / 3;
        this.BLOCK_HEIGHT = height / 3;
    }
}
