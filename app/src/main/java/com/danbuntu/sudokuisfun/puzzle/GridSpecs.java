package com.danbuntu.sudokuisfun.puzzle;

/**
 * Created by Dan on 4/21/2016. Have a great day!
 */
public class GridSpecs {

    public final float LEFT, TOP, RIGHT, BOTTOM;
    public final float GRID_HEIGHT, GRID_WIDTH;
    public final float ROW_HEIGHT, COL_WIDTH;
    public final static int ROWS = 9;
    public final static int COLS = 9;
    public final float BLOCK_WIDTH, BLOCK_HEIGHT;

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
