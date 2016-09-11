package com.danbuntu.androidsudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by Dan on 4/21/2016.
 */
public class SudokuGridView extends View implements View.OnTouchListener {

    //TODO add semi-transparent cover when puzzle is paused, also add pause and start methods

    GridSpecs DIMS;
    Paint thinLinePaint, thickLinePaint, highlight, selected, textPaint, pressed, locked, possibilityPaint;
    Coord coord;
    OnCellChangedListener cellChangedListener;
    Cell[][] mCellArray;
    int selectedDigit;
    boolean highlightNumbers = true, highlightNeighbors = true;
    boolean measured;
    boolean draw = true;

    public SudokuGridView(Context context) {
        super(context);
        init();
    }

    public SudokuGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SudokuGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(Math.min(widthMeasureSpec, heightMeasureSpec), Math.min(widthMeasureSpec, heightMeasureSpec));
    }

    private void init() {
        Log.i("SUDOKUGRIDVIEW", "INIT() CALLED");

        coord = new Coord();

        highlight = new Paint();
        highlight.setStyle(Paint.Style.FILL);
        highlight.setColor(getResources().getColor(R.color.cellHighlighted));

        thinLinePaint = new Paint();
        thinLinePaint.setStyle(Paint.Style.STROKE);
        thinLinePaint.setStrokeWidth(3f);
        thinLinePaint.setColor(getResources().getColor(R.color.thinGridLine));

        thickLinePaint = new Paint();
        thickLinePaint.setStyle(Paint.Style.STROKE);
        thickLinePaint.setStrokeWidth(8f);
        thickLinePaint.setColor(getResources().getColor(R.color.thickGridLine));

        selected = new Paint();
        selected.setStyle(Paint.Style.FILL);
        selected.setColor(getResources().getColor(R.color.cellSelected));

        pressed = new Paint();
        pressed.setStyle(Paint.Style.FILL);
        pressed.setColor(getResources().getColor(R.color.cellPressed));

        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(getResources().getColor(R.color.gridValueTextColor));
        textPaint.setTextAlign(Paint.Align.CENTER);

        possibilityPaint = new Paint();
        possibilityPaint.setStyle(Paint.Style.FILL);
        possibilityPaint.setColor(getResources().getColor(R.color.gridPossibilityTextColor));
        possibilityPaint.setTextAlign(Paint.Align.CENTER);

        locked = new Paint();
        locked.setStyle(Paint.Style.FILL);
        locked.setColor(getResources().getColor(R.color.cellLocked));

        setOnTouchListener(this);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                DIMS = new GridSpecs(getWidth(), getHeight());

                thinLinePaint.setStrokeWidth(getWidth() / 200f);
                thickLinePaint.setStrokeWidth(getWidth() / 80);

                Log.i("SUDOKUGRIDVIEW", "VIEW WIDTH: " + DIMS.GRID_WIDTH + " VIEW HEIGHT: " + DIMS.GRID_HEIGHT);
                measured = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

    }

    public void bindToCells(Cell[][] array) {
        mCellArray = array;
    }

    public Coord getSelectedCellCoords() {
        return coord;
    }

    private void drawGridLines(Canvas canvas) {

        // draw horizontal thin lines
        for (int i = 1; i < DIMS.ROWS; i++) {

            canvas.drawLine(DIMS.LEFT,
                    i * DIMS.ROW_HEIGHT,
                    DIMS.RIGHT,
                    i * DIMS.ROW_HEIGHT,
                    thinLinePaint);
        }

        // draw vertical thin lines
        for (int i = 1; i < DIMS.COLS; i++) {

            canvas.drawLine(i * DIMS.COL_WIDTH,
                    DIMS.TOP,
                    i * DIMS.COL_WIDTH,
                    DIMS.BOTTOM,
                    thinLinePaint);
        }

        canvas.drawLine(DIMS.LEFT,
                DIMS.ROW_HEIGHT * 3,
                DIMS.RIGHT,
                DIMS.ROW_HEIGHT * 3,
                thickLinePaint);
        canvas.drawLine(DIMS.LEFT,
                DIMS.ROW_HEIGHT * 6,
                DIMS.RIGHT,
                DIMS.ROW_HEIGHT * 6,
                thickLinePaint);
        canvas.drawLine(DIMS.COL_WIDTH * 3,
                DIMS.TOP,
                DIMS.COL_WIDTH * 3,
                DIMS.BOTTOM,
                thickLinePaint);
        canvas.drawLine(DIMS.COL_WIDTH * 6,
                DIMS.TOP,
                DIMS.COL_WIDTH * 6,
                DIMS.BOTTOM,
                thickLinePaint);
    }

    private void highlightCell(int x, int y, Canvas canvas, Paint paint) {
        canvas.drawRect(x * DIMS.COL_WIDTH,
                y * DIMS.ROW_HEIGHT,
                (x * DIMS.COL_WIDTH) + DIMS.COL_WIDTH,
                (y * DIMS.ROW_HEIGHT) + DIMS.ROW_HEIGHT, paint);
    }

    private void drawHighlight(Canvas canvas) {
        if (coord.getX() < 0 || coord.getY() < 0 || coord.getX() > 8 || coord.getY() > 8) return;

        // highlight row
        canvas.drawRect(DIMS.LEFT,
                coord.getY() * DIMS.ROW_HEIGHT,
                DIMS.RIGHT,
                (coord.getY() * DIMS.ROW_HEIGHT) + DIMS.ROW_HEIGHT,
                highlight);

        // highlight column
        canvas.drawRect(coord.getX() * DIMS.COL_WIDTH,
                DIMS.TOP,
                (coord.getX() * DIMS.COL_WIDTH) + DIMS.COL_WIDTH,
                DIMS.BOTTOM, highlight);

        // highlight block
        canvas.drawRect((coord.getX() / 3) * (DIMS.COL_WIDTH * 3),
                (coord.getY() / 3) * (DIMS.ROW_HEIGHT * 3),
                ((coord.getX() / 3) * (DIMS.COL_WIDTH * 3)) + (DIMS.COL_WIDTH * 3),
                ((coord.getY() / 3) * (DIMS.ROW_HEIGHT * 3)) + (DIMS.ROW_HEIGHT * 3), highlight);


    }

    private void highlightNumbers(int number, Canvas canvas, Paint paint) {
        for (int y = 0; y < DIMS.ROWS; y++) {
            for (int x = 0; x < DIMS.COLS; x++) {

                if (mCellArray[y][x].getValue() != -1 && mCellArray[y][x].getValue() == number && !(x == coord.x && y == coord.y))
                    highlightCell(x, y, canvas, paint);

            }
        }
    }

    private void drawCells(Canvas canvas) {

        if (mCellArray == null) return;


        Rect bounds = new Rect();

        for (int y = 0; y < mCellArray.length; y++) {

            for (int x = 0; x < mCellArray[0].length; x++) {

                if (mCellArray[y][x].getValue() != -1) {

                    float cellCenterX = (x * DIMS.COL_WIDTH) + (DIMS.COL_WIDTH / 2);
                    float cellCenterY = (y * DIMS.ROW_HEIGHT) + (DIMS.ROW_HEIGHT / 2);

                    // boldface selected digit
                    if (coord.x == x && coord.y == y) {
                        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        textPaint.setTextSize(DIMS.ROW_HEIGHT * 0.8f);
                    } else {
                        textPaint.setTypeface(Typeface.DEFAULT);
                        textPaint.setTextSize(DIMS.ROW_HEIGHT * 0.7f);
                    }

                    textPaint.getTextBounds(String.valueOf(mCellArray[y][x].getValue()), 0, 1, bounds);

                    canvas.drawText(String.valueOf(mCellArray[y][x].getValue()),
                            cellCenterX,
                            cellCenterY + (bounds.height() / 2),
                            textPaint);

                } else if (mCellArray[y][x].getPossibilities().size() > 0) {

                    float cellColThird = DIMS.COL_WIDTH / 3;
                    float cellRowThird = DIMS.ROW_HEIGHT / 3;

                    float cellLeft = (x * DIMS.COL_WIDTH) + (cellColThird / 2);
                    float cellTop = (y * DIMS.ROW_HEIGHT) + (cellRowThird / 2);

                    possibilityPaint.setTextSize(DIMS.ROW_HEIGHT * 0.3f);
                    for (int i : mCellArray[y][x].getPossibilities()) {

                        possibilityPaint.getTextBounds(String.valueOf(i), 0, 1, bounds);

                        canvas.drawText(String.valueOf(i),
                                cellLeft + (((i - 1) % 3) * cellColThird),
                                (cellTop + (((i - 1) / 3) * cellRowThird)) + (bounds.height() / 2),
                                possibilityPaint);

                    }

                }


            }

        }

    }

    public void setHighlightNumbers(boolean set) {
        highlightNumbers = set;
        invalidate();
    }

    public void setHighlightNeighbors(boolean set) {
        highlightNeighbors = set;
        invalidate();
    }

    private void drawLocked(Canvas canvas) {
        for (int y = 0; y < DIMS.ROWS; y++) {
            for (int x = 0; x < DIMS.COLS; x++) {
                if (mCellArray[y][x].isLocked()) highlightCell(x, y, canvas, locked);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(mCellArray != null && coord.y > -1 && coord.x > -1) selectedDigit = mCellArray[coord.y][coord.x].getValue();

        if (measured && draw) {

            drawLocked(canvas);

            canvas.saveLayerAlpha(0, 0, DIMS.GRID_WIDTH, DIMS.GRID_HEIGHT, 120,
                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

            if (highlightNeighbors) {
                drawHighlight(canvas);
            }

            if (highlightNumbers) {
                highlightNumbers(selectedDigit, canvas, selected);
            }

            if (coord.x != -1 && coord.y != -1) {
                highlightCell(coord.x, coord.y, canvas, pressed);
            }

            canvas.restore();

            drawGridLines(canvas);

            drawCells(canvas);

        } else if(measured) {

            drawLocked(canvas);
            drawGridLines(canvas);
            drawCells(canvas);

        }

        super.onDraw(canvas);


    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getActionMasked()) {

            case (MotionEvent.ACTION_DOWN):
            case (MotionEvent.ACTION_MOVE):
                coord.setX((int) (event.getX() / DIMS.COL_WIDTH));
                coord.setY((int) (event.getY() / DIMS.ROW_HEIGHT));
                invalidate();
                break;
        }

        return true;
    }

    public void drawBlankGrid(Canvas canvas) {
        draw = false;

        float originalThin;
        float originalThick;
        int originalThinColor = thinLinePaint.getColor();
        int originalThickColor = thickLinePaint.getColor();

        thinLinePaint.setStrokeWidth((originalThin = thinLinePaint.getStrokeWidth()) * 2f);
        thickLinePaint.setStrokeWidth((originalThick = thickLinePaint.getStrokeWidth()) * 2f);

        draw(canvas);

        thinLinePaint.setStrokeWidth(originalThin);
        thickLinePaint.setStrokeWidth(originalThick);

        draw = true;
    }

    public void setOnCellChangedListener(OnCellChangedListener listener) {
        cellChangedListener = listener;
    }

    public static class Coord {
        private int x = -1;
        private int y = -1;

        public void setX(int x) {
            if (x > 8 || x < 0) return;
            this.x = x;
        }

        public void setY(int y) {
            if (y > 8 || y < 0) return;
            this.y = y;
        }

        public void moveX(int n) {
            x += n;

            if (x > 8) {
                x = 8;
            } else if (x < 0) x = 0;

            if (y < 0) y = 0;
        }

        public void moveY(int n) {
            y += n;

            if (y > 8) {
                y = 8;
            } else if (y < 0) y = 0;

            if (x < 0) x = 0;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public interface OnCellChangedListener {
        boolean cellValueUpdated(Coord coord, int value);

        void cellValueCleared(Coord coord);
    }
}
