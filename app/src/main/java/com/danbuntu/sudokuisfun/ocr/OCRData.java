package com.danbuntu.sudokuisfun.ocr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

import com.danbuntu.sudokuisfun.puzzle.GridSpecs;
import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;
import com.danbuntu.sudokuisfun.utils.ThisBackupAgent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dan on 5/5/2016. Have a great day!
 */
public class OCRData {

    final static String HORIZONTAL_COUNT_SIGNATURE = "hc";
    final static String VERTICAL_COUNT_SIGNATURE = "vc";
    final static String TOP_MARGIN_SIGNATURE = "tm";
    final static String BOTTOM_MARGIN_SIGNATURE = "bm";
    final static String LEFT_MARGIN_SIGNATURE = "lm";
    final static String RIGHT_MARGIN_SIGNATURE = "rm";
    final static String HORIZONTAL_LINE_COUNT = "hlc";
    final static String VERTICAL_LINE_COUNT = "vlc";

    private int[] grid;
    private String externalOCRDataFile;
    private Context context;
    private Bitmap gridBitmap;
    private HashMap<String, DataContainer> digitData;
    private ScanListener mListener;

    private static String[] allSignatures = {
            HORIZONTAL_COUNT_SIGNATURE,
            VERTICAL_COUNT_SIGNATURE,
            TOP_MARGIN_SIGNATURE,
            BOTTOM_MARGIN_SIGNATURE,
            LEFT_MARGIN_SIGNATURE,
            RIGHT_MARGIN_SIGNATURE,
            HORIZONTAL_LINE_COUNT,
            VERTICAL_LINE_COUNT
    };

    public OCRData(Context context, String path) {
        this.context = context;
        this.externalOCRDataFile = SudokuUtils.getExternalOCRFile(context).getAbsolutePath();

        initOCRData();

        if (loadImage(path)) {
            Log.i("OCRData", "Successfully loaded sudoku grid image");
        } else {
            Log.e("OCRData", "Failed to load sudoku grid image");
        }
        grid = new int[GridSpecs.COLS * GridSpecs.ROWS];
        Arrays.fill(grid, -1);
    }

    private boolean loadImage(String path) {
        if (path != null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inMutable = true;
            gridBitmap = BitmapFactory.decodeFile(path, opts);

            if(gridBitmap == null) {
                Log.e("OCRData", "Loaded bitmap is null");
                return false;

            } else if(gridBitmap.getWidth() < OCR.optimalSide * GridSpecs.COLS || gridBitmap.getHeight() < OCR.optimalSide * GridSpecs.ROWS)
                Log.w("OCRData", "Warning, this image has lower than optimal resolution");

            return true;
        } else {
            return false;
        }
    }

    private void initOCRData() {

        digitData = new HashMap<>();
        createBlankHashMap();
        StringBuilder sb = new StringBuilder();

        try {
            Log.i("OCRDATA", "Attempting to load ocrdata asset now");
            byte[] buffer = new byte[1024];
            InputStream is = context.getAssets().open("ocrdata");
            int count;

            while ((count = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, count));
            }

            is.close();

            // add default OCR data (the asset file)
            Log.i("OCRDATA", "Finished loading signatures from asset file");
            addSigs(sb.toString());
            sb = new StringBuilder();

            // don't crash into the backup truck
            synchronized(ThisBackupAgent.sSyncLock) {
                if (externalOCRDataFile != null && new File(externalOCRDataFile).exists()) {

                    Log.i("OCRDATA", "Found external OCR data file, attempting to load it now");

                    FileInputStream fis = new FileInputStream(externalOCRDataFile);
                    while ((count = fis.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, count));
                    }
                    fis.close();

                    // add external OCR data (the learning file)
                    addSigs(sb.toString());
                    Log.i("OCRDATA", "Finished loading signatures from external signature file");

                } else {
                    Log.i("OCRDATA", "Could not find external OCR data file, skipping");
                }
            }


            Log.i("OCRDATA", "Finished loading signatures into hashmap");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("OCRDATA", "ERROR accessing ocrdata asset");
        }
    }

    public Bitmap getGridBitmap() {
        return gridBitmap;
    }

    public int[] getGridData() {
        return grid;
    }

    private void addSigs(String jsonString) {
        try {

            JSONObject main = new JSONObject(jsonString);
            Iterator<String> digits = main.keys();

            int signatures = 0;
            while (digits.hasNext()) {

                String key = digits.next();

                JSONObject digitRawData = main.getJSONObject(key);

                // this will append the data to the existing container
                digitData.get(key).parse(digitRawData);
                signatures++;
            }

            Log.i("OCRDATA", "Loaded " + signatures + " signatures into hashmap");

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("OCRDATA", "Failed to load signatures into hashmap");
        }
    }

    /**
     * Creates the base HashMap to store the ocrdata for quick access
     */
    private void createBlankHashMap() {
        for (int i = 1; i <= 9; i++) {
            DataContainer container = new DataContainer();
            digitData.put(String.valueOf(i), container);
        }
    }

    public void save(int x, int y, final int value) {
        final OCR ocr = new OCR(getRegionBitmap(x, y));
        new Thread(new Runnable() {
            @Override
            public void run() {
                ocr.prepare();
                ocr.setValue(value);
                try {
                    OCRData.this.save(ocr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * @param ocr OCR object that successfully identified a digit, or was supplied one via setValue
     */
    public void save(OCR ocr) throws IOException {
        if (ocr.getValue() == OCR.UNRECOGNIZED) return;

        // put on a reflective vest so the backup train doesn't hit you
        synchronized(ThisBackupAgent.sSyncLock) {
            File outFile;
            if (externalOCRDataFile != null) {
                outFile = new File(externalOCRDataFile);
                if (!outFile.exists() && !outFile.createNewFile()) {
                    // if you get here, the file object doesn't exist or can't be created
                    throw new IOException();
                }
            } else {
                // if you get here, we couldn't get an ocr file when this OCRData class was constructed
                throw new IOException();
            }

            String val = String.valueOf(ocr.getValue());

            addAll(digitData.get(val).get(HORIZONTAL_COUNT_SIGNATURE), ocr.horizontalCountSignature);

            addAll(digitData.get(val).get(VERTICAL_COUNT_SIGNATURE), ocr.verticalCountSignature);

            addAll(digitData.get(val).get(TOP_MARGIN_SIGNATURE), ocr.topMarginSignatures);

            addAll(digitData.get(val).get(BOTTOM_MARGIN_SIGNATURE), ocr.bottomMarginSignatures);

            addAll(digitData.get(val).get(LEFT_MARGIN_SIGNATURE), ocr.leftMarginSignatures);

            addAll(digitData.get(val).get(RIGHT_MARGIN_SIGNATURE), ocr.rightMarginSignatures);

            addAll(digitData.get(val).get(HORIZONTAL_LINE_COUNT), ocr.horizontalLineCountSignature);

            addAll(digitData.get(val).get(VERTICAL_LINE_COUNT), ocr.verticalLineCountSignature);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int sigCount = prefs.getInt(context.getString(R.string.pref_key_externalSigCount), 0);
            SharedPreferences.Editor prefsEdit = prefs.edit();
            prefsEdit.putInt(context.getString(R.string.pref_key_externalSigCount), sigCount + 1);
            prefsEdit.apply();

            FileOutputStream fos = new FileOutputStream(outFile);
            try {

                JSONObject object = new JSONObject();
                for (String key : digitData.keySet()) {

                    JSONObject o = new JSONObject();
                    for (String subKey : digitData.get(key).keySet()) {

                        JSONArray array = new JSONArray();
                        for (int i : digitData.get(key).get(subKey)) {
                            array.put(i);
                        }

                        o.put(subKey, array);
                    }
                    object.put(key, o);
                }

                fos.write(object.toString().getBytes());

                Log.i("OCRData", "Successfully saved data for digit with value of: " + ocr.getValue());

            } catch (JSONException e) {
                Log.e("OCRData", "Failed to save data for digit with value of: " + ocr.getValue());
                throw new IOException();
            }

            fos.close();
        }

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

    public void scan(OCR ocr) {
        scan(ocr, false);
    }

    public void scan(OCR ocr, boolean independent) {

        if (ocr.isBlank()) return;

        float[] certainty = new float[9];

        scanEachDigitSignatures(ocr.horizontalCountSignature, HORIZONTAL_COUNT_SIGNATURE, certainty);
        scanEachDigitSignatures(ocr.verticalCountSignature, VERTICAL_COUNT_SIGNATURE, certainty);

        scanEachDigitSignatures(ocr.topMarginSignatures, TOP_MARGIN_SIGNATURE, certainty);
        scanEachDigitSignatures(ocr.bottomMarginSignatures, BOTTOM_MARGIN_SIGNATURE, certainty);
        scanEachDigitSignatures(ocr.leftMarginSignatures, LEFT_MARGIN_SIGNATURE, certainty);
        scanEachDigitSignatures(ocr.rightMarginSignatures, RIGHT_MARGIN_SIGNATURE, certainty);

        scanEachDigitSignatures(ocr.horizontalLineCountSignature, HORIZONTAL_LINE_COUNT, certainty);
        scanEachDigitSignatures(ocr.verticalLineCountSignature, VERTICAL_LINE_COUNT, certainty);

        float lowestVal = Float.MAX_VALUE;
        int foundDigit = OCR.UNRECOGNIZED;
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
        Log.i("OCRData", "Found digit with value of " + foundDigit + " with a " + digitCertainty + " percent certainty");

        if (!independent) {
            grid[ocr.index] = ocr.getValue();
        }

    }

    private void scanEachDigitSignatures(int[] ints, String signatureType, float[] certainty) {
        for (int digit = 1; digit <= 9; digit++)
            certainty[digit - 1] += compareSignatureToRawData(ints, digitData.get(String.valueOf(digit)).get(signatureType));
    }

    private float compareSignatureToRawData(int[] current, ArrayList<Integer> saved) {

        int count = 0;
        float total = 0;

        for (int i = 0; i < saved.size(); i += OCR.optimalSide) {

            if (i + OCR.optimalSide <= saved.size()) {

                float compare = compareSignatures(saved, current, i);

                if (compare != -1) {
                    total += compare;
                    count++;
                }

            } else {
                Log.e("OCRDATA", "Warning, signature data file appears to have incorrect length, most likely corrupted data");
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
        if(gridBitmap == null) return;

        final float xStep = (float) gridBitmap.getWidth() / GridSpecs.COLS;
        final float yStep = (float) gridBitmap.getHeight() / GridSpecs.ROWS;

        int poolSize = Runtime.getRuntime().availableProcessors();
        Log.i("OCRData", "Creating threadPool using the: " + poolSize + " available processors on this system");

        Bitmap cell;
        final ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);

        for (int y = 0; y < GridSpecs.ROWS; y++) {

            for (int x = 0; x < GridSpecs.COLS; x++) {

                cell = Bitmap.createBitmap(gridBitmap,
                        (int)(x * xStep),
                        (int)(y * yStep),
                        (int)xStep,
                        (int)yStep);

                if (cell != null) {
                    threadPool.submit(
                            new OCRRunnable(new OCR(cell, (y * GridSpecs.COLS) + x), this)
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
            Log.e("OCRData", "Threadpool encountered an interruption");
        }
    }

    public Bitmap getRegionBitmap(int x, int y) {
        if(x < 0 || y < 0 || x > 8 || y > 8) return null;
        float bW = gridBitmap.getWidth() / 9f;
        float bH = gridBitmap.getHeight() / 9f;
        int bX = (int)(x * bW);
        int bY = (int)(y  * bH);
        return Bitmap.createBitmap(gridBitmap, bX, bY, (int)bW, (int)bH);
    }

    private class DataContainer {

        HashMap<String, ArrayList<Integer>> data;

        public DataContainer() {
            initNew();
        }

        public DataContainer(JSONObject jsonObject) {
            initNew();
            parse(jsonObject);
        }

        private void initNew() {
            data = new HashMap<>();
            for (String key : OCRData.allSignatures) {
                data.put(key, new ArrayList<Integer>());
            }
        }

        public void parse(JSONObject jsonObject) {

            for (String key : OCRData.allSignatures) {

                if (jsonObject != null && jsonObject.has(key)) {
                    ArrayList<Integer> sArray = data.get(key);

                    try {
                        JSONArray array = jsonObject.getJSONArray(key);

                        for (int i = 0; i < array.length(); i++) {
                            sArray.add(array.getInt(i));
                        }

                    } catch (JSONException e) {
                        Log.e("OCRDATA", "Problem parsing JSON");
                    }
                }
            }
        }

        public boolean isValid() {
            return data != null;
        }

        public boolean containsKey(String key) {
            return data.containsKey(key);
        }

        public ArrayList<Integer> get(String sig) {
            return data.get(sig);
        }

        public Set<String> keySet() {
            return data.keySet();
        }

        public int size() {
            if (isValid()) {
                return data.size();
            } else {
                return 0;
            }
        }
    }

    private class OCRRunnable implements Runnable {

        OCR ocr;
        OCRData parent;

        public OCRRunnable(OCR ocr, OCRData parent) {
            this.ocr = ocr;
            this.parent = parent;
        }

        @Override
        public void run() {
            ocr.prepare();
            parent.scan(ocr);
            if(mListener != null) mListener.onCellFinished();
        }
    }

    public void setScanListener(ScanListener listener) {
        this.mListener = listener;
    }

    public interface ScanListener {
        void onCellFinished();
    }
}
