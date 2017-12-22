package com.danbuntu.sudokuisfun.ocr;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dan on 12/17/17. Have a great day!
 */
public class DataContainer extends HashMap<String, ArrayList<Integer>> {

    public DataContainer() {
        initNew();
    }

    public DataContainer(JSONObject jsonObject) {
        initNew();
        parseDataChunk(jsonObject);
    }

    private void initNew() {
        for (String key : OCRScanner.allSignatures) {
            put(key, new ArrayList<Integer>());
        }
    }

    public void parseDataChunk(JSONObject jsonObject) {

        if(jsonObject == null) return;

        for (String key : OCRScanner.allSignatures) {

            if(!jsonObject.has(key)) continue;

            try {

                Object obj = jsonObject.get(key);
                get(key).addAll(parseInsideObject(obj));

            } catch (JSONException e) {
                Log.e("OCRDATA", "Problem parsing JSON");
            }

        }
    }

    /**
     * @param object Should be either a Base64 encoded string or a JSONArray
     * @return ArrayList of integers that were either encoded as Base64 or as a JSONArray
     * @throws JSONException if the JSON is garbage
     */
    private ArrayList<Integer> parseInsideObject(Object object) throws JSONException {

        ArrayList<Integer> output = new ArrayList<>();

        if(object instanceof JSONArray) {

            Log.i("DataContainer", "Loading a JSON string that was saved in array format");
            JSONArray array = (JSONArray) object;
            for(int i=0; i<array.length(); i++) {
                output.add((Integer) array.get(i));
            }

        } else {

            Log.i("DataContainer", "Loading a JSON string that was saved in base64 string format");
            byte[] decodedBytes = Base64.decode(object.toString(), Base64.DEFAULT);
            for(byte b : decodedBytes) {
                output.add(b & 0xff);
            }

        }

        return output;
    }

    private static String compress(ArrayList<Integer> arrayList) {
        byte[] bytes = new byte[arrayList.size()];
        for(int i=0; i<arrayList.size(); i++) {
            // this works as long as the OCRUnit.optimalSide <= 255
            bytes[i] = (byte) (arrayList.get(i) & 0xff);
        }
        return new String(Base64.encode(bytes, Base64.DEFAULT));
    }

    private static ArrayList<Integer> decompress(String dataString) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        byte[] bytes = Base64.decode(dataString, Base64.DEFAULT);
        for(byte b : bytes) {
            arrayList.add(b & 0xff);
        }
        return arrayList;
    }

    JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        for(String key : keySet()) {
            try {
                obj.put(key, compress(get(key)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    static DataContainer fromJSON(String jsonString) {
        DataContainer dc = new DataContainer();
        try {
            dc.parseDataChunk(new JSONObject(jsonString));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dc;
    }
}
