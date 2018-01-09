package com.danbuntu.sudokuisfun.ocr;

import android.content.Context;
import android.util.Log;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.ThisBackupAgent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by dan on 12/17/17. Have a great day!
 */

public class DataManager {

    private static int BUFFER_LENGTH = 1024;
    private Context mContext;
    private String externalOCRDataFile;
    HashMap<String, DataContainer> digitData;

    DataManager(Context context) {
        mContext = context;
        digitData = new HashMap<>();
        externalOCRDataFile = getExternalDataFile(mContext).getAbsolutePath();
        init();
    }

    private void init() {
        for (int i = 1; i <= 9; i++) {
            DataContainer container = new DataContainer();
            digitData.put(String.valueOf(i), container);
        }
    }

    DataContainer getDataForNumber(int n) {
        return getDataForNumber(String.valueOf(n));
    }

    DataContainer getDataForNumber(String n) {
        if(digitData.containsKey(n)) {
            return digitData.get(n);
        } else {
            return null;
        }
    }

    void loadSignatures() {
        String data;

        // only load signatures from the external data file
        // if it exists ignore the asset file
        if((data = getDataFileString()) != null) {
            addSignaturesFromString(data);

        } else if((data = getAssetString()) != null) {
            addSignaturesFromString(data);
        }
    }

    public static File getExternalDataFile(Context context) {
        return new File(context.getFilesDir(), context.getString(R.string.filename_ocr));
    }

    private void addSignaturesFromString(String jsonString) {
        if(jsonString == null || jsonString.equals(""))
            return;

        try {

            JSONObject main = new JSONObject(jsonString);
            Iterator<String> digits = main.keys();

            int signatures = 0;
            while (digits.hasNext()) {

                String key = digits.next();

                JSONObject digitRawData = main.getJSONObject(key);

                // this will append the dataMap to the existing container
                digitData.get(key).parseDataChunk(digitRawData);
                signatures++;
            }

            Log.i("DataManager", "Loaded " + signatures + " signatures");

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DataManager", "Failed to load signatures into hashmap");
        }
    }

    /**
     * @return Json data from the included assets data file, null if it can't be loaded
     */
    private String getAssetString() {

        StringBuilder sb = new StringBuilder();

        try {

            byte[] buffer = new byte[BUFFER_LENGTH];
            int count;
            
            InputStream is = mContext.getAssets().open("ocrdata");

            while ((count = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, count));
            }
            
            is.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    /**
     * @return Json data from external data file, null if the file doesn't exist
     */
    private String getDataFileString() {

        StringBuilder sb = new StringBuilder();
        try {

            byte[] buffer = new byte[BUFFER_LENGTH];
            int count;
            
            if (externalOCRDataFile != null && new File(externalOCRDataFile).exists()) {

                synchronized(ThisBackupAgent.sSyncLock) {
                    Log.i("DataManager", "Found external OCR data file, loading");

                    FileInputStream fis = new FileInputStream(externalOCRDataFile);
                    while ((count = fis.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, count));
                    }
                    fis.close();
                }
                return sb.toString();

            } else {
                Log.i("DataManager", "Could not find external ocr data file, skipping");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    void save() throws IOException {

        synchronized(ThisBackupAgent.sSyncLock) {

            if (externalOCRDataFile == null)
                throw new IOException();

            // create the learning file if it doesn't exist
            File outFile = new File(externalOCRDataFile);
            if (!outFile.exists() && !outFile.createNewFile()) {
                // if you get here, the file object doesn't exist or can't be created
                throw new IOException();
            }

            // write the dataMap to the file
            FileOutputStream fos = new FileOutputStream(outFile);
            try {
                String str = toJSON();
                fos.write(str.getBytes("UTF-8"));
                Log.i("DataManager", "Successfully saved " + externalOCRDataFile);

            } catch (JSONException e) {
                Log.i("DataManager", "Failed to save " + externalOCRDataFile);
                throw new IOException();
            }
            fos.close();
        }
    }

    String toJSON() throws JSONException {
        JSONObject object = new JSONObject();

        for (String key : digitData.keySet()) {
            object.put(key, digitData.get(key).toJSON());
        }

        return object.toString();
    }

}