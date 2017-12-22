package com.danbuntu.sudokuisfun.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.danbuntu.sudokuisfun.puzzle.GridSpecs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dan on 5/5/2016. Have a great day!
 */
public class OCRScanner {

    final static String HORIZONTAL_COUNT_PREFIX = "hc";
    final static String VERTICAL_COUNT_PREFIX = "vc";
    final static String TOP_MARGIN_PREFIX = "tm";
    final static String BOTTOM_MARGIN_PREFIX = "bm";
    final static String LEFT_MARGIN_PREFIX = "lm";
    final static String RIGHT_MARGIN_PREFIX = "rm";
    final static String HORIZONTAL_LC_PREFIX = "hlc";
    final static String VERTICAL_LC_PREFIX = "vlc";
    static String[] allSignatures = {
            HORIZONTAL_COUNT_PREFIX,
            VERTICAL_COUNT_PREFIX,
            TOP_MARGIN_PREFIX,
            BOTTOM_MARGIN_PREFIX,
            LEFT_MARGIN_PREFIX,
            RIGHT_MARGIN_PREFIX,
            HORIZONTAL_LC_PREFIX,
            VERTICAL_LC_PREFIX
    };
    private Context mContext;
    private int[] grid;
    private Bitmap gridBitmap;
    private ScanListener mListener;
    private DataManager mDataManager;

    public OCRScanner(Context context, String path) {
        this.mContext = context;

        // load the OCRUnit dataMap
        mDataManager = new DataManager(mContext);
        mDataManager.loadSignatures();

        if (loadImage(path)) {
            Log.i("OCRScanner", "Successfully loaded sudoku grid image");
        } else {
            Log.e("OCRScanner", "Failed to load sudoku grid image");
        }

        grid = new int[GridSpecs.COLS * GridSpecs.ROWS];
        Arrays.fill(grid, -1);
    }

    private boolean loadImage(String path) {
        if (path != null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inMutable = true;
            gridBitmap = BitmapFactory.decodeFile(path, opts);

            if (gridBitmap == null) {
                Log.e("OCRScanner", "Loaded bitmap is null");
                return false;

            } else if (gridBitmap.getWidth() < OCRUnit.optimalSide * GridSpecs.COLS || gridBitmap.getHeight() < OCRUnit.optimalSide * GridSpecs.ROWS)
                Log.w("OCRScanner", "Warning, this image has lower than optimal resolution");

            return true;
        } else {
            return false;
        }
    }

    public Bitmap getGridBitmap() {
        return gridBitmap;
    }

    public int[] getGridData() {
        return grid;
    }

    public void saveLearnedOCRData(int x, int y, final int value) {
        final OCRUnit ocr = new OCRUnit(getRegionBitmap(x, y));
        new Thread(new Runnable() {
            @Override
            public void run() {
                ocr.prepare();
                ocr.setValue(value);
                try {
                    OCRScanner.this.saveLearnedOCRData(ocr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void saveLearnedOCRData(OCRUnit ocr) throws IOException {
        if (ocr.getValue() == OCRUnit.UNRECOGNIZED)
            return;

        // get the dataMap container for this digit
        DataContainer digitData = mDataManager.getDataForNumber(ocr.getValue());

        // add signatures to the dataMap container
        addAll(digitData.get(HORIZONTAL_COUNT_PREFIX), ocr.horizontalCountSignature);
        addAll(digitData.get(VERTICAL_COUNT_PREFIX), ocr.verticalCountSignature);
        addAll(digitData.get(TOP_MARGIN_PREFIX), ocr.topMarginSignatures);
        addAll(digitData.get(BOTTOM_MARGIN_PREFIX), ocr.bottomMarginSignatures);
        addAll(digitData.get(LEFT_MARGIN_PREFIX), ocr.leftMarginSignatures);
        addAll(digitData.get(RIGHT_MARGIN_PREFIX), ocr.rightMarginSignatures);
        addAll(digitData.get(HORIZONTAL_LC_PREFIX), ocr.horizontalLineCountSignature);
        addAll(digitData.get(VERTICAL_LC_PREFIX), ocr.verticalLineCountSignature);

        // write the dataMap to file
        mDataManager.save();

    }

    private void addAll(ArrayList<Integer> list, int[] array) {
        for (int i : array) list.add(i);
    }

    /**
     * @param master master list for comparison
     * @param check  array to check against master
     * @param offset the index in master to start comparing with check
     * @return returns the average difference between the corresponding digits in each list
     */
    private float compareSignatures(ArrayList<Integer> master, int[] check, int offset) {
        if (offset + check.length > master.size()) {
            Log.e("OCRDATA", "Comparing signatures failed, offset: " + offset + "(+" + check.length + ") length: " + master.size());
            return -1;
        }

        int diff = 0;

        for (int i = 0; i < check.length; i++) {
            diff += Math.abs((byte) (master.get(i + offset) & 0xFF) - check[i]);
        }

        return (float) diff / check.length;
    }

    public void scan(OCRUnit ocr) {
        scan(ocr, false);
    }

    public void scan(OCRUnit ocr, boolean independent) {

        if (ocr.isBlank()) return;

        float[] certainty = new float[9];

        scanEachDigitSignatures(ocr.horizontalCountSignature, HORIZONTAL_COUNT_PREFIX, certainty);
        scanEachDigitSignatures(ocr.verticalCountSignature, VERTICAL_COUNT_PREFIX, certainty);

        scanEachDigitSignatures(ocr.topMarginSignatures, TOP_MARGIN_PREFIX, certainty);
        scanEachDigitSignatures(ocr.bottomMarginSignatures, BOTTOM_MARGIN_PREFIX, certainty);
        scanEachDigitSignatures(ocr.leftMarginSignatures, LEFT_MARGIN_PREFIX, certainty);
        scanEachDigitSignatures(ocr.rightMarginSignatures, RIGHT_MARGIN_PREFIX, certainty);

        scanEachDigitSignatures(ocr.horizontalLineCountSignature, HORIZONTAL_LC_PREFIX, certainty);
        scanEachDigitSignatures(ocr.verticalLineCountSignature, VERTICAL_LC_PREFIX, certainty);

        float lowestVal = Float.MAX_VALUE;
        int foundDigit = OCRUnit.UNRECOGNIZED;
        float digitCertainty = 0;

        float total = 0;
        for (float aCertainty : certainty) {
            total += aCertainty;
        }

        for (int i = 0; i < certainty.length; i++) {
            // convert to percentage
            certainty[i] = certainty[i] / total;

            if (certainty[i] < lowestVal) {
                lowestVal = certainty[i];
                foundDigit = i + 1;
                digitCertainty = (1f - certainty[i]) * 100f;
            }
        }

        ocr.setValue(foundDigit);
        Log.i("OCRScanner", "Found digit with value of " + foundDigit + " with a " + digitCertainty + " percent certainty");

        if (!independent) {
            grid[ocr.index] = ocr.getValue();
        }

    }

    private void scanEachDigitSignatures(int[] ints, String signatureType, float[] certainty) {
        for (int digit = 1; digit <= 9; digit++)
            certainty[digit - 1] += compareSignatureToRawData(ints, mDataManager.digitData.get(String.valueOf(digit)).get(signatureType));
    }

    private float compareSignatureToRawData(int[] current, ArrayList<Integer> saved) {

        int count = 0;
        float total = 0;

        for (int i = 0; i < saved.size(); i += OCRUnit.optimalSide) {

            if (i + OCRUnit.optimalSide <= saved.size()) {

                float compare = compareSignatures(saved, current, i);

                if (compare != -1) {
                    total += compare;
                    count++;
                }

            } else {
                Log.e("OCRDATA", "Warning, signature data file appears to have incorrect length, most likely corrupted data file");
            }
        }

        if (count != 0) {
            total = total / count;
            return total;
        } else {
            return -1;
        }

    }

    public void beginScan() {
        if (gridBitmap == null) return;

        final float xStep = (float) gridBitmap.getWidth() / GridSpecs.COLS;
        final float yStep = (float) gridBitmap.getHeight() / GridSpecs.ROWS;

        int poolSize = Runtime.getRuntime().availableProcessors();
        Log.i("OCRScanner", "Creating threadPool using the: " + poolSize + " available processors on this system");

        Bitmap cell;
        final ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);

        for (int y = 0; y < GridSpecs.ROWS; y++) {

            for (int x = 0; x < GridSpecs.COLS; x++) {

                cell = Bitmap.createBitmap(gridBitmap,
                        (int) (x * xStep),
                        (int) (y * yStep),
                        (int) xStep,
                        (int) yStep);

                if (cell != null) {
                    threadPool.submit(
                            new OCRRunnable(new OCRUnit(cell, (y * GridSpecs.COLS) + x), this)
                    );
                }
            }
        }

        threadPool.shutdown();
        try {
            // 3 min timeout on the thread pool
            if (!threadPool.awaitTermination(3, TimeUnit.MINUTES)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.e("OCRScanner", "Threadpool encountered an interruption");
        }
    }

    public Bitmap getRegionBitmap(int x, int y) {
        if (x < 0 || y < 0 || x > 8 || y > 8) return null;
        float bW = gridBitmap.getWidth() / 9f;
        float bH = gridBitmap.getHeight() / 9f;
        int bX = (int) (x * bW);
        int bY = (int) (y * bH);
        return Bitmap.createBitmap(gridBitmap, bX, bY, (int) bW, (int) bH);
    }

    public void setScanListener(ScanListener listener) {
        this.mListener = listener;
    }

    public interface ScanListener {
        void onCellFinished();
    }

    private class OCRRunnable implements Runnable {

        OCRUnit ocr;
        OCRScanner parent;

        public OCRRunnable(OCRUnit ocr, OCRScanner parent) {
            this.ocr = ocr;
            this.parent = parent;
        }

        @Override
        public void run() {
            ocr.prepare();
            parent.scan(ocr);
            if (mListener != null) mListener.onCellFinished();
        }
    }
}
