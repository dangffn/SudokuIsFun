package com.danbuntu.androidsudoku;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Dan on 4/19/2016.
 */
public class Cell {
    ArrayList<Integer> possibilities;
    int UNKNOWN = -1;
    int value = UNKNOWN;
    int x, y, z = -1;
    boolean locked;

    public Cell(int x, int y) {
        this.possibilities = new ArrayList<>();
        this.x = x;
        this.y = y;
        this.z = ((y / (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x / (GridSpecs.COLS / 3));
    }

    public void setValue(int value) {
        if (!locked) {
            this.value = value;
            possibilities = new ArrayList<>();
        }
    }

    public void setValue(int value, boolean force) {
        if(force && isLocked()) lock(false);
        setValue(value);
    }

    public void lock(boolean set) {
        if (set) {
            if (getValue() != -1) {
                locked = set;
                possibilities = new ArrayList<>();
            }
        } else {
            locked = set;
        }

    }

    public boolean isLocked() {
        return locked;
    }

    public void setPossibilities(ArrayList<Integer> possibilities) {
        if (possibilities != null && getValue() == UNKNOWN && !locked)
            this.possibilities = possibilities;
    }

    public ArrayList<Integer> getPossibilities() {
        return possibilities;
    }

    public void setPosibility(int val) {
        if (locked || val < 1 || val > 9) return;

        if (possibilities.contains(val)) {
            possibilities.remove(possibilities.indexOf(val));
        } else {
            possibilities.add(val);
        }
    }

    public void removePossibility(int val) {
        if (locked) return;

        if (possibilities.contains(val)) {
            possibilities.remove(possibilities.indexOf(val));
        }
    }

    public int getRow() {
        return y;
    }

    public int getCol() {
        return x;
    }

    public int getBlk() {
        return z;
    }

    public int getIndex() {
        return (y * GridSpecs.COLS) + x;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return (value == -1) ? "" : String.valueOf(value);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        obj.put("V", getValue());
        obj.put("L", isLocked());
        for(int i : getPossibilities()) array.put(i);
        obj.put("P", array);

        return obj;
    }

    public void loadJSON(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        value = obj.getInt("V");
        possibilities = new ArrayList<>();
        JSONArray a = obj.getJSONArray("P");
        for(int i=0; i<a.length(); i++) {
            int x = a.getInt(i);
            if(x >0 && x<=9)
                possibilities.add(a.getInt(i));
        }
        locked = obj.getBoolean("L");
    }
}
