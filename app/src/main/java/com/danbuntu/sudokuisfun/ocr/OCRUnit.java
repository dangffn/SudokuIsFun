package com.danbuntu.sudokuisfun.ocr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Dan on 5/3/2016. Have a great day!
 */
public class OCRUnit {

    final static int optimalSide = 60;

    final static int darkestWhite = 80;
    final static int maxFragmentSize = 20;
    final static float fullThreshold = 0.80f;

    int[] leftMarginSignatures, rightMarginSignatures, topMarginSignatures, bottomMarginSignatures;
    int[] horizontalCountSignature, verticalCountSignature;
    int[] horizontalLineCountSignature, verticalLineCountSignature;
    int blackPixels, whitePixels;
    int leftMargin, rightMargin, topMargin, bottomMargin;
    int blackWidth, blackHeight;
    int index;
    float weightRatio, widthToHeightRatio;

    final static int UNRECOGNIZED = -1;
    int VALUE = UNRECOGNIZED;

    boolean BLANK = true;

    Bitmap sourceBitmap;

    public OCRUnit(Bitmap inputBitmap) {
        this(inputBitmap, -1);
    }

    /**
     * @param inputBitmap The bitmap to search for a single digit in
     * @param index The index of this image in the grid
     */
    public OCRUnit(Bitmap inputBitmap, int index) {
        sourceBitmap = inputBitmap;

        VALUE = UNRECOGNIZED;

        leftMarginSignatures = new int[optimalSide];
        rightMarginSignatures = new int[optimalSide];
        topMarginSignatures = new int[optimalSide];
        bottomMarginSignatures = new int[optimalSide];

        horizontalCountSignature = new int[optimalSide];
        verticalCountSignature = new int[optimalSide];

        horizontalLineCountSignature = new int[optimalSide];
        verticalLineCountSignature = new int[optimalSide];

        this.index = index;
    }

    public void convertGrayscale() {
        if (!bitmapIsAlive()) return;
        Canvas canvas = new Canvas(sourceBitmap);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(sourceBitmap, 0, 0, paint);
    }

    public void setValue(int value) {
        if (value < 1 || value > 9) return;

        VALUE = value;
    }

    /**
     * Fills in black pixels on sharp corners. Joins pixel groups if they are connected with 2
     * black pixels diagonally. Prevents the groups from being filtered out as fragments.
     */
    public void soften() {

        if(!bitmapIsAlive()) return;

        Queue<Point> queue = new LinkedList<>();

        short[][] checked = new short[sourceBitmap.getHeight()][sourceBitmap.getWidth()];

        // queue up all the black pixels to be checked
        for (int y = 1; y < sourceBitmap.getHeight() - 1; y++) {
            for (int x = 1; x < sourceBitmap.getWidth() - 1; x++) {
                if (sourceBitmap.getPixel(x, y) == Color.BLACK) {
                    queue.add(new Point(x, y));
                }
            }
        }

        Point p;
        while (!queue.isEmpty()) {
            p = queue.remove();

            if (checked[p.y][p.x] == 3) continue;

            checked[p.y][p.x] = 3;

            checked[p.y][p.x - 1]++;
            checked[p.y - 1][p.x]++;
            checked[p.y][p.x + 1]++;
            checked[p.y + 1][p.x]++;
        }

        for (int y = 0; y < checked.length; y++) {
            for (int x = 0; x < checked[0].length; x++) {
                // turn white pixels black if the have atleast 2 black pixels touching
                if(checked[y][x] >= 2) sourceBitmap.setPixel(x, y, Color.BLACK);
            }
        }
    }

    public int getValue() {
        return VALUE;
    }

    /**
     * Converts the bitmap to black and white. Based on hardcoded darkestWhite value.
     */
    public void binarize() {
        if (!bitmapIsAlive()) return;

        for (int y = 0; y < sourceBitmap.getHeight(); y++) {
            for (int x = 0; x < sourceBitmap.getWidth(); x++) {

                int pix = sourceBitmap.getPixel(x, y);
                int avg = (pix >> 16) & 0xff;
                avg += (pix >> 8) & 0xff;
                avg += pix & 0xff;
                avg /= 3;

                if (avg < darkestWhite) {
                    sourceBitmap.setPixel(x, y, Color.BLACK);
                } else {
                    sourceBitmap.setPixel(x, y, Color.WHITE);
                }

            }
        }
    }

    public int averageArray(int[] array) {
        int total = 0;
        for (int i : array) total += i;
        return total / array.length;
    }

    /**
     * Removes any leftover pieces of a square grid border from the edges
     * of the image to leave only the character and whitespace.
     */
    public void trimBorders() {

        if (!bitmapIsAlive()) return;

        Queue<Point> fillQueue = new LinkedList<>();

        boolean[][] checked = new boolean[sourceBitmap.getHeight()][sourceBitmap.getWidth()];


        // clean leftover border fragments from top and bottom
        for (int x = 0; x < sourceBitmap.getWidth(); x++) {

            fill(fillQueue, new Point(x, 0), checked, true);
            fill(fillQueue, new Point(x, sourceBitmap.getHeight() - 1), checked, true);

        }

        // clean leftover border fragments from left and right
        for (int y = 0; y < sourceBitmap.getHeight(); y++) {

            fill(fillQueue, new Point(0, y), checked, true);
            fill(fillQueue, new Point(sourceBitmap.getWidth() - 1, y), checked, true);

        }


    }

    /**
     * Remove any group of black pixels if the count of black pixels is smaller than maxFragmentSize
     */
    public void fragmentScan() {

        boolean[][] checked = new boolean[sourceBitmap.getHeight()][sourceBitmap.getWidth()];
        Queue<Point> fragmentQueue = new LinkedList<>();

        Queue<Point> fillQueue = new LinkedList<>();

        Point checkPoint;

        for (int y = 0; y < sourceBitmap.getHeight(); y++) {
            for (int x = 0; x < sourceBitmap.getWidth(); x++) {

                if (sourceBitmap.getPixel(x, y) == Color.BLACK && !checked[y][x]) {

                    checkPoint = new Point(x, y);
                    int size = fill(fillQueue, checkPoint, checked, false);

                    // queue up the fragment for deletion
                    if (size <= maxFragmentSize) {
                        fragmentQueue.add(checkPoint);
                    }

                }
            }
        }

        checked = new boolean[sourceBitmap.getHeight()][sourceBitmap.getWidth()];
        while (!fragmentQueue.isEmpty()) {
            // actually clean the fragments under the threshold size
            fill(fragmentQueue, null, checked, true);
        }

    }

    /**
     * @param fillQueue queue of pixels that still have to be checked
     * @param p the point in the array to start at
     * @param checked 2d array representing which pixels were already checked
     * @param modify whether or not to turn the black pixels white
     * @return number of black pixels in a connected group
     */
    private int fill(Queue<Point> fillQueue, Point p, boolean[][] checked, boolean modify) {

        int count = 0;

        if (p != null)
            fillQueue.add(p);

        while (!fillQueue.isEmpty()) {

            p = fillQueue.remove();

            if (!checked[p.y][p.x] && sourceBitmap.getPixel(p.x, p.y) == Color.BLACK) {

                if (modify) sourceBitmap.setPixel(p.x, p.y, Color.WHITE);
                count++;

                if (!((p.x - 1) < 0))
                    fillQueue.add(new Point(p.x - 1, p.y));
                if (!((p.y - 1) < 0))
                    fillQueue.add(new Point(p.x, p.y - 1));
                if (!((p.x + 1) >= sourceBitmap.getWidth()))
                    fillQueue.add(new Point(p.x + 1, p.y));
                if (!((p.y + 1) >= sourceBitmap.getHeight()))
                    fillQueue.add(new Point(p.x, p.y + 1));

            }

            checked[p.y][p.x] = true;
        }
        return count;
    }

    /**
     * Re-sizes the image to the preferred height and width
     */
    public void resizeToPreferred() {
        if (bitmapIsAlive()) {
            sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, OCRUnit.optimalSide, OCRUnit.optimalSide, false);
        }
    }

    private boolean bitmapIsAlive() {
        return sourceBitmap != null && !sourceBitmap.isRecycled();
    }

    public Bitmap getBitmap() {
        if (!bitmapIsAlive()) {
            return null;
        } else {
            return sourceBitmap;
        }
    }

    public boolean isBlank() {
        return BLANK;
    }

    public void scanMargins() {
        if (!bitmapIsAlive()) return;

        leftMargin = rightMargin = optimalSide - 1;
        topMargin = bottomMargin = optimalSide - 1;

        for (int y = 0; y < optimalSide; y++) {

            for (int x = 0; x < optimalSide; x++) {

                if (sourceBitmap.getPixel(x, y) == Color.BLACK) {

                    // finds the margin of whitespace
                    if (y < topMargin) topMargin = y;
                    if (optimalSide - y < bottomMargin) bottomMargin = optimalSide - y;
                    if (x < leftMargin) leftMargin = x;
                    if (optimalSide - x < rightMargin) rightMargin = optimalSide - x;

                }
            }
        }

        // size of the black area
        blackWidth = (optimalSide - rightMargin) - leftMargin;
        blackHeight = (optimalSide - bottomMargin) - topMargin;

        BLANK = blackWidth <= 0 || blackHeight <= 0;
    }

    /**
     * Calculates all of the statistics on the image needed for signature comparison
     */
    public void scanSignatures() {

        if (BLANK || !bitmapIsAlive()) return;
        if (sourceBitmap.getHeight() != optimalSide || sourceBitmap.getWidth() != optimalSide)
            return;

        // first pixel in rows and columns
        for (int i = 0; i < topMarginSignatures.length; i++)
            topMarginSignatures[i] = optimalSide - 1;
        for (int i = 0; i < leftMarginSignatures.length; i++)
            leftMarginSignatures[i] = optimalSide - 1;

        for (int y = 0; y < optimalSide; y++) {

            for (int x = 0; x < optimalSide; x++) {

                if (sourceBitmap.getPixel(x, y) == Color.BLACK) {

                    // finds the first pixel in the row / column for each side
                    if (y < topMarginSignatures[x]) topMarginSignatures[x] = y;
                    if (y > bottomMarginSignatures[x]) bottomMarginSignatures[x] = y;
                    if (x < leftMarginSignatures[y]) leftMarginSignatures[y] = x;
                    if (x > rightMarginSignatures[y]) rightMarginSignatures[y] = x;

                    // increment horizontal / vertical pixel count
                    horizontalCountSignature[y]++;
                    verticalCountSignature[x]++;

                    if (x == 0 || sourceBitmap.getPixel(x - 1, y) == Color.WHITE)
                        horizontalLineCountSignature[y]++;
                    if (y == 0 || sourceBitmap.getPixel(x, y - 1) == Color.WHITE)
                        verticalLineCountSignature[x]++;

                    // ratio of black to white pixels
                    blackPixels++;
                }


            }


        }

        // ratio of black to white pixels
        weightRatio = (float) blackPixels / (optimalSide * optimalSide);

        whitePixels = (optimalSide * optimalSide) - blackPixels;

        // ratio of width to height
        widthToHeightRatio = (float) blackWidth / blackHeight;

        if (weightRatio >= fullThreshold) BLANK = true;

    }

    public void fillObjectToImage() {

        if (BLANK) return;

        // extract the object from the image
        sourceBitmap = Bitmap.createBitmap(sourceBitmap, leftMargin, topMargin, blackWidth, blackHeight);

        // resize the object to fill the image bounds
        sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, optimalSide, optimalSide, false);

    }

    /**
     * Prepares the bitmap for OCRUnit scanning, discards loaded bitmap upon completion
     */
    public void prepare() {
        prepare(false);
    }

    /**
     * Prepares the bitmap for OCRUnit scanning
     * @param persist if true, the bitmap will not be recycled after complete
     */
    public void prepare(boolean persist) {

        if(!bitmapIsAlive()) {
            Log.e("OCRDATA", "Attempted to run on a null bitmap, returning");
            return;
        }

        // convert black and white
        binarize();

        // resize to preferred working dimensions
        resizeToPreferred();

        // remove border artifacts
        trimBorders();

        // soften
        soften();

        // find and clean the noise
        fragmentScan();

        // find the object in the image
        scanMargins();

        if (!isBlank()) {
            // fill the object to the size of the image
            fillObjectToImage();

            // scan the signatures on the clean image
            scanSignatures();
        }

        if(!persist) {
            sourceBitmap.recycle();
        }

    }

    public void clearBitmap() {
        if (bitmapIsAlive()) {
            sourceBitmap.recycle();
            sourceBitmap = null;
        }
    }
}
