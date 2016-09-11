package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.puzzle.Cell;
import com.danbuntu.sudokuisfun.puzzle.GridSpecs;

/**
 * Created by Dan on 4/21/2016. Have a great day!
 */
public class SudokuGridView extends View implements View.OnTouchListener {

    GridSpecs DIMS;
    Paint thinLinePaint, thickLinePaint, highlight, selected,
            textPaint, pressed, lockedPaint, hintPaint,
            possibilityPaint, alertPaint, white, clr;
    Coord coord;
    Cell[][] mCellArray;
    int selectedDigit;
    boolean highlightNumbers = true, highlightNeighbors = true;
    boolean measured;
    boolean editMode;

    float thickerLine = 10f, thickLine = 8f, thinLine = 3f;

    final static int MODE_NUMBERS = 0;
    final static int MODE_LETTERS = 1;
    final static int MODE_COLORS = 2;
    private int CURRENT_DISPLAY_MODE = MODE_NUMBERS;
    private int REQUESTED_DISPLAY_MODE = MODE_NUMBERS;

    private String[] letters;
    private int[] colors;

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
        int min = Math.min(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(min, min);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(Color.WHITE);

        if (mCellArray != null && coord.y > -1 && coord.x > -1)
            selectedDigit = mCellArray[coord.y][coord.x].getValue();

        if (measured && !editMode) {

            if(CURRENT_DISPLAY_MODE != MODE_COLORS) {

                drawLocked(canvas, lockedPaint);

                if (coord.x != -1 && coord.y != -1) {

                    canvas.saveLayerAlpha(0, 0, DIMS.GRID_WIDTH, DIMS.GRID_HEIGHT, 120,
                            Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

                    if (highlightNeighbors) {
                        drawHighlight(canvas);
                    }

                    if (highlightNumbers) {
                        highlightNumbers(selectedDigit, canvas, selected);
                    }

                    highlightCell(coord.x, coord.y, canvas, pressed);

                    canvas.restore();
                }

                drawCells(canvas, true);

                drawGridLines(canvas);

                drawAlerts(canvas, alertPaint);

                drawHints(canvas, hintPaint);

            } else {

                // COLOR MODE!!! WOO HOO
                canvas.saveLayerAlpha(0, 0, DIMS.GRID_WIDTH, DIMS.GRID_HEIGHT, 150,
                        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
                drawCells(canvas, true);
                canvas.restore();

                drawGridLines(canvas);

                drawLocked(canvas, lockedPaint);

                drawAlerts(canvas, alertPaint);

                if (coord.x != -1 && coord.y != -1) highlightCell(coord.x, coord.y, canvas, pressed);

            }

        } else if (measured && editMode) {

            if (CURRENT_DISPLAY_MODE != MODE_COLORS) {

                canvas.drawColor(lockedPaint.getColor());

                drawLocked(canvas, white);

                drawGridLines(canvas);

                drawCells(canvas, true);

                drawLocked(canvas, hintPaint);

                if (coord.x != -1 && coord.y != -1) {
                    canvas.saveLayerAlpha(0, 0, DIMS.GRID_WIDTH, DIMS.GRID_HEIGHT, 120, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
                    highlightCell(coord.x, coord.y, canvas, pressed);
                    canvas.restore();
                }

                drawAlerts(canvas, alertPaint);

            }

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

    public void setDisplayMode(int mode) {
        if (!inEditMode() && CURRENT_DISPLAY_MODE != mode) {
            if (mode == MODE_LETTERS) {
                switchToLetterMode();
            } else if (mode == MODE_COLORS) {
                switchToColorMode();
            } else if (mode == MODE_NUMBERS) {
                switchToNumberMode();
            }
        }
        REQUESTED_DISPLAY_MODE = mode;
    }

    public int getDisplayMode() {
        return CURRENT_DISPLAY_MODE;
    }

    private void switchToColorMode() {
        colors = getContext().getResources().getIntArray(R.array.colors);
        if (colors != null && measured) {
            CURRENT_DISPLAY_MODE = MODE_COLORS;
            selected.setStyle(Paint.Style.STROKE);
            selected.setStrokeWidth(thickLine);
            highlight.setStyle(Paint.Style.STROKE);
            highlight.setStrokeWidth(thickLine);
            pressed.setStyle(Paint.Style.STROKE);
            pressed.setStrokeWidth(thickLine);
            lockedPaint.setStyle(Paint.Style.STROKE);
            lockedPaint.setStrokeWidth(thinLine);
            lockedPaint.setColor(Color.BLACK);
        } else if(colors != null) {
            // this will be set up in the global layout listener
            CURRENT_DISPLAY_MODE = MODE_COLORS;
        }
    }

    private void switchToLetterMode() {
        letters = getContext().getResources().getStringArray(R.array.letters);
        if (letters != null) {
            CURRENT_DISPLAY_MODE = MODE_LETTERS;
            initPaint();
        }
    }

    private void switchToNumberMode() {
        CURRENT_DISPLAY_MODE = MODE_NUMBERS;
        initPaint();
    }

    private void init() {
        init(true);
    }

    private void init(boolean willBeVisible) {
        Log.i("SUDOKUGRIDVIEW", "init() Called");

        coord = new Coord();

        initPaint();

        if(willBeVisible) {

            setOnTouchListener(this);

            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    measured = true;

                    adjustToSize(getWidth(), getHeight());

                    if (CURRENT_DISPLAY_MODE == MODE_COLORS) {
                        // this requires the view to be measured so call it here
                        switchToColorMode();
                    }

                    Log.i("SUDOKUGRIDVIEW", "VIEW WIDTH: " + DIMS.GRID_WIDTH + " VIEW HEIGHT: " + DIMS.GRID_HEIGHT);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
        }
    }

    private void adjustToSize(int width, int height) {
        DIMS = new GridSpecs(width, height);

        thinLine = width / 200f;
        thinLinePaint.setStrokeWidth(thinLine);

        thickLine = width / 80f;
        thickerLine = thickLine + (thickLine / 4);
        thickLinePaint.setStrokeWidth(thickLine);
        alertPaint.setStrokeWidth(thickLine);
        hintPaint.setStrokeWidth(thickerLine);
    }

    private void initPaint() {
        alertPaint = new Paint();
        alertPaint.setStyle(Paint.Style.STROKE);
        alertPaint.setStrokeWidth(thickLine);
        alertPaint.setColor(getResources().getColor(R.color.grid_border_alert));

        hintPaint = new Paint();
        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setStrokeWidth(thickerLine);
        hintPaint.setColor(getResources().getColor(R.color.grid_cell_hint));

        highlight = new Paint();
        highlight.setStyle(Paint.Style.FILL);
        highlight.setColor(getResources().getColor(R.color.grid_cell_highlighted));

        thinLinePaint = new Paint();
        thinLinePaint.setStyle(Paint.Style.STROKE);
        thinLinePaint.setStrokeWidth(thinLine);
        thinLinePaint.setColor(getResources().getColor(R.color.grid_line_thin));

        thickLinePaint = new Paint();
        thickLinePaint.setStyle(Paint.Style.STROKE);
        thickLinePaint.setStrokeWidth(thickLine);
        thickLinePaint.setColor(getResources().getColor(R.color.grid_line_thick));

        selected = new Paint();
        selected.setStyle(Paint.Style.FILL);
        selected.setColor(getResources().getColor(R.color.grid_cell_selected));

        pressed = new Paint();
        pressed.setStyle(Paint.Style.FILL);
        pressed.setColor(getResources().getColor(R.color.grid_cell_pressed));

        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(getResources().getColor(R.color.grid_text_value));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        possibilityPaint = new Paint();
        possibilityPaint.setStyle(Paint.Style.FILL);
        possibilityPaint.setColor(getResources().getColor(R.color.grid_text_possibility));
        possibilityPaint.setTextAlign(Paint.Align.CENTER);
        possibilityPaint.setAntiAlias(true);

        lockedPaint = new Paint();
        lockedPaint.setStyle(Paint.Style.FILL);
        lockedPaint.setColor(getResources().getColor(R.color.grid_cell_locked));

        clr = new Paint();
        clr.setStyle(Paint.Style.FILL);

        white = new Paint();
        white.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
    }

    public void bindToCells(Cell[][] array) {
        mCellArray = array;
    }

    public Coord getSelectedCellCoords() {
        return coord;
    }

    public void setSelectedCellCoords(int x, int y) {
        coord.setX(x);
        coord.setY(y);
        invalidate();
    }

    private void drawGridLines(Canvas canvas) {

        // draw horizontal thin lines
        for (int i = 1; i < GridSpecs.ROWS; i++) {

            canvas.drawLine(DIMS.LEFT,
                    i * DIMS.ROW_HEIGHT,
                    DIMS.RIGHT,
                    i * DIMS.ROW_HEIGHT,
                    thinLinePaint);
        }

        // draw vertical thin lines
        for (int i = 1; i < GridSpecs.COLS; i++) {

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

    private void drawCross(int x, int y, Canvas canvas, Paint paint) {
        canvas.drawLine(x * DIMS.COL_WIDTH,
                y * DIMS.ROW_HEIGHT,
                (x * DIMS.COL_WIDTH) + DIMS.COL_WIDTH,
                (y * DIMS.ROW_HEIGHT) + DIMS.ROW_HEIGHT, paint);

        canvas.drawLine(x * DIMS.COL_WIDTH,
                (y * DIMS.ROW_HEIGHT) + DIMS.ROW_HEIGHT,
                (x * DIMS.COL_WIDTH) + DIMS.COL_WIDTH,
                y * DIMS.ROW_HEIGHT, paint);
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
        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {

                if (mCellArray[y][x].getValue() != -1 && mCellArray[y][x].getValue() == number && !(x == coord.x && y == coord.y))
                    highlightCell(x, y, canvas, paint);

            }
        }
    }

    public void setEditMode(boolean editMode) {
        if(editMode == this.editMode) return;

        this.editMode = editMode;
        if(editMode && CURRENT_DISPLAY_MODE != MODE_NUMBERS) {

            // switch to number mode since were editing
            REQUESTED_DISPLAY_MODE = CURRENT_DISPLAY_MODE;
            switchToNumberMode();

        } else if(!editMode && REQUESTED_DISPLAY_MODE != MODE_NUMBERS) {

            // revert back to the previous mode that we were in before we changed to edit mode
            switch (REQUESTED_DISPLAY_MODE) {
                case(MODE_COLORS):
                    switchToColorMode();
                    break;
                case(MODE_LETTERS):
                    switchToLetterMode();
                    break;
            }

//            REQUESTED_DISPLAY_MODE = MODE_NUMBERS;
        }
        invalidate();
    }

    private void drawCells(Canvas canvas, boolean boldSelected) {

        if (mCellArray == null) return;


        Rect bounds = new Rect();
        String character;

        for (int y = 0; y < mCellArray.length; y++) {

            for (int x = 0; x < mCellArray[0].length; x++) {

                int val = mCellArray[y][x].getValue();
                if (val <= 9 && val >= 1) {

                    float cellCenterX = (x * DIMS.COL_WIDTH) + (DIMS.COL_WIDTH / 2);
                    float cellCenterY = (y * DIMS.ROW_HEIGHT) + (DIMS.ROW_HEIGHT / 2);

                    if (CURRENT_DISPLAY_MODE == MODE_COLORS) {

                        int color = colors[val - 1];
                        clr.setColor(color);
                        highlightCell(x, y, canvas, clr);

                    } else {

                        if (boldSelected && coord.x == x && coord.y == y) {
                            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                            textPaint.setTextSize(DIMS.ROW_HEIGHT * 0.8f);
                        } else {
                            textPaint.setTypeface(Typeface.DEFAULT);
                            textPaint.setTextSize(DIMS.ROW_HEIGHT * 0.7f);
                        }

                        if (CURRENT_DISPLAY_MODE == MODE_LETTERS) {
                            character = letters[val - 1];
                        } else {
                            character = String.valueOf(val);
                        }
                        textPaint.getTextBounds(character, 0, 1, bounds);

                        canvas.drawText(character,
                                cellCenterX,
                                cellCenterY + (bounds.height() / 2),
                                textPaint);
                    }

                } else if (mCellArray[y][x].getPossibilities().size() > 0) {

                    float cellColThird = DIMS.COL_WIDTH / 3;
                    float cellRowThird = DIMS.ROW_HEIGHT / 3;
                    float cellX = x * DIMS.COL_WIDTH;
                    float cellY = y * DIMS.ROW_HEIGHT;
                    float cellLeft = cellX + (cellColThird / 2);
                    float cellTop = cellY + (cellRowThird / 2);

                    possibilityPaint.setTextSize(DIMS.ROW_HEIGHT * 0.3f);
                    for (int i : mCellArray[y][x].getPossibilities()) {

                        if (CURRENT_DISPLAY_MODE == MODE_COLORS) {

                            int color = colors[i - 1];
                            clr.setColor(color);
                            float l = cellX + (cellColThird * ((i - 1) % 3));
                            float t = cellY + (cellRowThird * ((i - 1) / 3));
                            canvas.drawRect(l, t, l + cellColThird, t + cellRowThird, clr);

                        } else {

                            if (CURRENT_DISPLAY_MODE == MODE_LETTERS) {
                                character = letters[i - 1];
                            } else {
                                character = String.valueOf(i);
                            }
                            possibilityPaint.getTextBounds(character, 0, 1, bounds);

                            canvas.drawText(character,
                                    cellLeft + (((i - 1) % 3) * cellColThird),
                                    (cellTop + (((i - 1) / 3) * cellRowThird)) + (bounds.height() / 2),
                                    possibilityPaint);
                        }
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

    private void drawAlerts(Canvas canvas, Paint paint) {
        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {
                if (mCellArray[y][x].isAlert()) highlightCell(x, y, canvas, paint);
            }
        }
    }

    private void drawLocked(Canvas canvas, Paint paint) {
        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {
                if (mCellArray[y][x].isLocked()) {
                    if(CURRENT_DISPLAY_MODE != MODE_COLORS) {
                        highlightCell(x, y, canvas, paint);
                    } else {
                        drawCross(x, y, canvas, paint);
                    }
                }
            }
        }
    }

    private void drawHints(Canvas canvas, Paint paint) {
        for (int y = 0; y < GridSpecs.ROWS; y++) {
            for (int x = 0; x < GridSpecs.COLS; x++) {
                if (mCellArray[y][x].isHint()) {
                    highlightCell(x, y, canvas, paint);
                }
            }
        }
    }

    public boolean inEditMode() {
        return editMode;
    }

    public Bitmap getSnapshot(int bitmapSize, boolean preserve) {
        return getSnapshot(bitmapSize, getWidth(), getHeight(), preserve);
    }

    public Bitmap getSnapshot(int bitmapSize, int w, int h, boolean preserve) {

        if(w <= 0 || h <= 0) return null;

        if(!measured) {
            init(false);
            adjustToSize(w, h);
        }

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bmp);

        drawSnapshot(canvas, preserve);

        bmp = Bitmap.createScaledBitmap(bmp, bitmapSize, bitmapSize, true);

        return bmp;
    }

    public void drawSnapshot(Canvas canvas, boolean saveEverything) {

        float originalThin;
        float originalThick;

        thinLinePaint.setStrokeWidth((originalThin = thinLinePaint.getStrokeWidth()) * 2f);
        thickLinePaint.setStrokeWidth((originalThick = thickLinePaint.getStrokeWidth()) * 2f);

        canvas.drawColor(Color.WHITE);

        if(CURRENT_DISPLAY_MODE == MODE_COLORS) {
            drawCells(canvas, false);
            drawLocked(canvas, lockedPaint);
        } else {
            drawLocked(canvas, lockedPaint);
            drawCells(canvas, false);
        }

        if (saveEverything) {

            canvas.saveLayerAlpha(0, 0, DIMS.GRID_WIDTH, DIMS.GRID_HEIGHT, 120,
                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

            if (highlightNeighbors) drawHighlight(canvas);

            if (highlightNumbers) highlightNumbers(selectedDigit, canvas, selected);

            if (coord.x != -1 && coord.y != -1) highlightCell(coord.x, coord.y, canvas, pressed);

            canvas.restore();

        }

        drawGridLines(canvas);

        thinLinePaint.setStrokeWidth(originalThin);
        thickLinePaint.setStrokeWidth(originalThick);
    }

    public static class Coord {
        private int x = -1;
        private int y = -1;

        public void setX(int x) {
            if (x > 8 || x < -1) return;
            this.x = x;
        }

        public void setY(int y) {
            if (y > 8 || y < -1) return;
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
}
