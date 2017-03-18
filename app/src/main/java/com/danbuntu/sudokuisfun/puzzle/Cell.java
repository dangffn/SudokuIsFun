package com.danbuntu.sudokuisfun.puzzle;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Dan on 4/19/2016.
 */
public class Cell {

    public final static int NONE = -1;
    final static int VAL_CHANGE = 1;
    final static int POS_CHANGE = 2;
    final static int LCK_CHANGE = 3;

    private ArrayList<Integer> possibilities;
    private HashMap<Short, byte[]> history;

    private int value;
    private int x, y, z;
    private boolean mLocked, mAlert, mHint;
    private Puzzle mPuzzle;

    public Cell(int x, int y, Puzzle parent) {
        this.possibilities = new ArrayList<>();
        history = new HashMap<>();
        this.x = x;
        this.y = y;
        this.z = ((y / (GridSpecs.ROWS / 3)) * (GridSpecs.COLS / 3)) + (x / (GridSpecs.COLS / 3));
        this.mPuzzle = parent;
        this.value = NONE;
    }

    public boolean setValue(int value, boolean force) {
        // set the value only if the value is a valid digit and its not locked and its not the same as the current value
        if ((!isLocked() || force) &&
                value <= 9 &&
                value >= 1 &&
                value != this.value) {

            if (possibilities.size() != 0) {

                // possibilities are getting removed, log the last possibilities
                log(possibilities);

            } else {

                // value is changing only, log the last value
                log(this.value);

            }

            int oldValue = this.value;
            this.value = value;
            possibilities = new ArrayList<>();

            if(oldValue == NONE) {
                mPuzzle.onCellValueSet(true);
            } else {
                this.value = value;
                mPuzzle.onCellValueSet();
            }
            return true;

        }
        return false;
    }

    public void setHint(boolean set) {
        this.mHint = set;
    }

    public boolean isHint() {
        return mHint;
    }

    public boolean setValue(int value) {
        return setValue(value, false);
    }

    public boolean addPossibility(int val) {
        if (mLocked || val < 1 || val > 9) return false;

        if (!possibilities.contains(val)) {
            log(possibilities);
            possibilities.add(val);
            return true;
        }
        return false;
    }

    public boolean removePossibility(int val) {
        if (possibilities.contains(val)) {
            log(possibilities);
            possibilities.remove(possibilities.indexOf(val));
            return true;
        }
        return false;
    }

    public boolean setPossibilities(ArrayList<Integer> possibilities) {
        boolean updated = false;
        if (possibilities != null && getValue() == NONE && !mLocked) {

            for(int i : possibilities) if(i>9 || i < 1) return false;

            if (this.possibilities.size() != 0) {

                // possibilities are getting removed, log the last possibilities
                log(this.possibilities);

            } else {

                // need to save this or else weird things happen when we solve
                log(NONE);

            }

            updated = !this.possibilities.equals(possibilities);
            this.possibilities = possibilities;
            return updated;
        }
        return updated;
    }

    public ArrayList<Integer> getPossibilities() {
        return possibilities;
    }

    public void setLocked(boolean set) {
        if (value != NONE) mLocked = set;
    }

    public boolean isLocked() {
        return mLocked;
    }

    public boolean clear(boolean force) {

        if(mLocked && !force) return false;

        mAlert = false;
        if(force) mLocked = false;

        if (value != NONE) {

            log(value);
            mPuzzle.onCellValueSet(false);

        } else if (possibilities.size() != 0) {

            log(possibilities);

        } else {

            // cell already was cleared, no update was made
            return false;

        }

        this.value = NONE;
        possibilities = new ArrayList<>();
        return true;

    }

    public boolean clear() {
        return clear(false);
    }

    public boolean notSet() {
        return value == NONE;
    }

    public int revertToStatus(short newRev) {
        byte[] data = history.remove(newRev);

        if (data == null) return NONE;

        int oldValue;

        switch ((int) data[0]) {

            case (POS_CHANGE):
                // possibilities recorded
                int returnVal = POS_CHANGE;
                if(value != NONE) {
                    mPuzzle.onCellValueSet(false);
                    returnVal = VAL_CHANGE;
                }
                value = NONE;
                possibilities = new ArrayList<>();
                for (int i = 1; i < data.length; i++) possibilities.add((int) data[i]);
                mLocked = false;

                return returnVal;

            case (VAL_CHANGE):
                // value recorded while unlocked
                oldValue = value;
                value = (int) data[1];
                possibilities = new ArrayList<>();

                if(oldValue == NONE && data[1] != NONE) {
                    mPuzzle.onCellValueSet(true);
                } else if(oldValue != NONE && data[1] == NONE) {
                    mPuzzle.onCellValueSet(false);
                } else if(oldValue != NONE && data[1] != NONE) {
                    mPuzzle.onCellValueSet();
                }
                mLocked = false;

                return VAL_CHANGE;

            case(LCK_CHANGE):
                // value recorded while locked
                oldValue = value;
                value = (int) data[1];
                possibilities = new ArrayList<>();

                if(oldValue == NONE && data[1] != NONE) {
                    mPuzzle.onCellValueSet(true);
                } else if(oldValue != NONE && data[1] == NONE) {
                    mPuzzle.onCellValueSet(false);
                } else if(oldValue != NONE && data[1] != NONE) {
                    mPuzzle.onCellValueSet();
                }
                mLocked = true;

                return VAL_CHANGE;

            default:
                return NONE;

        }
    }

    private void log(ArrayList<Integer> list) {
        short rev = mPuzzle.getCurrentRevision();

        if (mPuzzle.inLogMode() && !history.containsKey(rev)) {

            byte[] data = new byte[list.size() + 1];
            data[0] = (byte) POS_CHANGE;
            for (int i = 0; i < list.size(); i++) data[i + 1] = (byte) ((int) list.get(i));
            history.put(rev, data);
        }

    }

    private void log(int number) {
        short rev = mPuzzle.getCurrentRevision();

        if (mPuzzle.inLogMode() && !history.containsKey(rev)) {

            byte b = (mLocked) ? (byte) LCK_CHANGE : (byte) VAL_CHANGE;
            byte[] data = new byte[]{b, (byte) number};
            history.put(rev, data);
            Log.i("Cell", "Logged revision: " + rev + " value: " + Arrays.toString(data) + " for cell: " + getIndex());
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
        return (value == NONE) ? "" : String.valueOf(value);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();

        // put value
        if(value != NONE) obj.put("V", value);

        // put locked boolean
        if(mLocked) obj.put("L", mLocked);

        // put alert boolean
        if(mAlert) obj.put("A", mAlert);

        // put possibilities
        if(possibilities.size() > 0) {
            JSONArray array = new JSONArray();
            for (int i : possibilities) array.put(i);
            obj.put("P", array);
        }

        // put history
        if(history.keySet().size() > 0) {
            JSONObject o = new JSONObject();
            for (short key : history.keySet()) {
                o.put(String.valueOf(key), Base64.encodeToString(history.get(key), Base64.NO_PADDING));
            }
            obj.put("H", o);
        }

        return obj;
    }

    public void loadJSON(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        loadJSON(obj);
    }

    public void loadJSON(JSONObject obj) throws JSONException {
        loadJSON(obj, true);
    }

    public void loadJSON(JSONObject obj, boolean overrideHistory) throws JSONException {

        // load value
        int newValue = (!obj.isNull("V")) ? obj.getInt("V") : NONE;

        // load possibilities
        ArrayList<Integer> newPossibilities = new ArrayList<>();
        if(!obj.isNull("P")) {
            JSONArray a = obj.getJSONArray("P");
            for (int i = 0; i < a.length(); i++) {
                int x = a.getInt(i);
                if (x > 0 && x <= 9)
                    newPossibilities.add(a.getInt(i));
            }
        }

        // load locked boolean
        boolean newLocked = (!obj.isNull("L")) && obj.getBoolean("L");

        // load alert boolean
        boolean newAlert = (!obj.isNull("A")) && obj.getBoolean("A");

        // load history
        HashMap<Short, byte[]> newHistory = new HashMap<>();
        if(!obj.isNull("H")){
            JSONObject o = obj.getJSONObject("H");
            Iterator<String> keyIterator = o.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                newHistory.put(Short.valueOf(key), Base64.decode(o.getString(key), Base64.NO_PADDING));
            }
        }

        if(overrideHistory) {
            history = newHistory;
        } else {

            if(possibilities.size() > 0) {
                log(possibilities);
            } else {
                log(value);
            }
        }

        if(value == NONE && newValue != NONE) {
            mPuzzle.onCellValueSet(true);
        } else if(value != NONE && newValue == NONE) {
            mPuzzle.onCellValueSet(false);
        }

        value = newValue;
        possibilities = newPossibilities;
        mLocked = newLocked;
        mAlert = newAlert;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cell && ((Cell) o).getIndex() == getIndex();
    }

    public void setAlert(boolean set) {
        mAlert = set;
    }

    public boolean isAlert() {
        return mAlert;
    }
}
