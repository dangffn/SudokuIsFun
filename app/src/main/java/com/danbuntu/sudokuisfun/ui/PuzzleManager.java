package com.danbuntu.sudokuisfun.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.puzzle.Cell;
import com.danbuntu.sudokuisfun.puzzle.Puzzle;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;
import com.danbuntu.sudokuisfun.utils.ThisBackupAgent;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/**
 * Created by Dan on 5/16/2016. Have a great day!
 */
public class PuzzleManager extends SQLiteAssetHelper {

    final static int DB_VERSION = 1;
    public final static String DB_NAME = "puzzles.db";
    final static String PUZZLES_TABLE = "puzzles";
    final static String STATS_TABLE = "stats";
    // puzzle data
    final static String PUZZLE_NAME_COL = "_id";
    final static String PUZZLE_DESC_COL = "puzzle_description";
    final static String PUZZLE_DATA_COL = "puzzle_data";
    final static String PUZZLE_ICON_COL = "puzzle_icon";
    final static String PUZZLES_CATEGORY_COL = "puzzle_category";
    final static String PUZZLE_ESTIMATED_DIFFICULTY = "estimated_difficulty";
    // saved data
    final static String SAVED_COL = "saved";
    final static String SAVE_ICON = "save_icon";
    final static String SAVE_DATA_COL = "save_data";
    final static String SAVE_X = "save_x";
    final static String SAVE_Y = "save_y";
    final static String SAVE_R = "save_r";
    final static String SAVE_DURATION = "save_duration";
    final static String SAVE_FINISHED = "save_finished";
    final static String SAVE_ASSISTED = "save_assisted";
    // statistics data
    final static String FROM_IMAGE = "from_image";
    final static String BEST_FINISH_TIME_UNASSISTED = "best_finish_time_unassisted";
    final static String BEST_FINISH_TIME_ASSISTED = "best_finish_time_assisted";
    final static String LONGEST_SESSION = "longest_session";
    final static String FINISH_COUNT = "finish_count";
    final static String COMPUTER_ASSIST_COUNT = "computer_assist_count";
    final static String TIMES_OPENED = "times_opened";
    final static String TOTAL_DURATION = "total_duration";
    final static int YES = 1;
    final static int NO = 0;
    static PuzzleManager sInstance;
    Context mContext;

    private PuzzleManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }

    public static PuzzleManager getInstance(Context context) {
        synchronized (ThisBackupAgent.sSyncLock) {
            Log.i("PUZZLEMANAGER", "Instance requested");
            if (sInstance == null) {
                sInstance = new PuzzleManager(context);
                Log.i("PUZZLEMANAGER", "New instance created");
            }
            return sInstance;
        }
    }

    @Deprecated
    public int[] getPuzzleData(String name) {
        return getPuzzleData(name, true);
    }

    public int[] getPuzzleData(String name, boolean newGame) {
        byte[] raw = null;
        SQLiteDatabase db = null;

        synchronized (ThisBackupAgent.sSyncLock) {
            db = getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + PUZZLE_DATA_COL + " FROM " + PUZZLES_TABLE + " WHERE " + PUZZLE_NAME_COL + " = ?", new String[]{name});
            if (cursor.moveToFirst()) {
                raw = cursor.getBlob(0);
            }
            cursor.close();
        }

        int[] template = null;
        if (raw != null) {
            template = new int[raw.length];
            for (int i = 0; i < template.length; i++)
                template[i] = (int) raw[i];
        }

        // increment this puzzles opened count
        if (newGame) incrementCol(db, STATS_TABLE, TIMES_OPENED, name);

        return template;
    }

    public Bitmap getPuzzleIcon(String puzzleName) {
        byte[] blob = null;

        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(PUZZLES_TABLE, new String[]{PUZZLE_ICON_COL,}, PUZZLE_NAME_COL + "=?", new String[]{puzzleName},
                    null, null, null);
            if (cursor.moveToFirst()) blob = cursor.getBlob(cursor.getColumnIndex(PUZZLE_ICON_COL));
            cursor.close();
        }

        if (blob == null) return null;

        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }

    public ArrayList<String> getAllPuzzleNames() {
        return getAllPuzzleNames(null);
    }

    public ArrayList<String> getAllPuzzleNames(String category) {
        ArrayList<String> all;

        synchronized(ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();

            Cursor cursor;
            if(category == null) {
                cursor = db.query(PuzzleManager.PUZZLES_TABLE, new String[]{ PuzzleManager.PUZZLE_NAME_COL },
                        null, null, null, null, null, null);
            } else {
                cursor = db.query(PuzzleManager.PUZZLES_TABLE, new String[]{ PuzzleManager.PUZZLE_NAME_COL },
                        PUZZLES_CATEGORY_COL + "=?", new String[]{ category }, null, null, null, null);
            }

            all = new ArrayList<>();
            int nameIndex = cursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL);
            if(cursor.moveToFirst()) {
                do {
                    all.add(cursor.getString(nameIndex));
                } while(cursor.moveToNext());
            }
            cursor.close();
        }

        return all;
    }

    private void incrementCol(SQLiteDatabase db, String table, String column, String puzzleName) {
        incrementCol(db, table, column, puzzleName, 1);
    }

    private void incrementCol(SQLiteDatabase db, String table, String column, String puzzleName, long value) {
        synchronized (ThisBackupAgent.sSyncLock) {
            // check to make sure the field isn't null, otherwise it wont increment
            String sqlIncrement = String.format(Locale.getDefault(), "UPDATE %s SET %s=%s+? WHERE %s=?",
                    table, column, column, PUZZLE_NAME_COL);
            db.execSQL(sqlIncrement, new Object[]{value, puzzleName});
        }
    }

    public boolean saveState(Bitmap bitmap, String puzzleName, Cell[] cells, int x, int y,
                             long sessionDuration, long timeSinceLastSave, int revision, boolean finished, boolean computerAssist) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();

            if (!containsPuzzle(db, puzzleName)) return false;

            String s;
            try {
                JSONArray j = new JSONArray();
                for (Cell cell : cells)
                    j.put(cell.toJSON());
                s = j.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }

            byte[] imgBytes = SudokuUtils.bitmapToBytes(bitmap);

            if (computerAssist) incrementCol(db, STATS_TABLE, COMPUTER_ASSIST_COUNT, puzzleName);
            incrementCol(db, STATS_TABLE, TOTAL_DURATION, puzzleName, timeSinceLastSave);
            incrementCol(db, PUZZLES_TABLE, SAVE_DURATION, puzzleName, timeSinceLastSave);
            updateStat(puzzleName, LONGEST_SESSION, false, sessionDuration);

            ContentValues values = new ContentValues();

            // if this save was already marked finished, don't increment the puzzle finish count, else increment it
            // (prevents having the finish count == the times the user pressed save on a finished puzzle)
            if (finished) {
                Cursor cursor = db.query(PUZZLES_TABLE, new String[]{SAVE_FINISHED}, PUZZLE_NAME_COL + "=?", new String[]{puzzleName},
                        null, null, null);
                boolean isFinished = false;
                int index = cursor.getColumnIndex(SAVE_FINISHED);
                if (cursor.moveToFirst() && !cursor.isNull(index)) {
                    isFinished = cursor.getInt(index) == YES;
                }
                cursor.close();

                if (!isFinished) {
                    incrementCol(db, STATS_TABLE, FINISH_COUNT, puzzleName);
                    values.put(SAVE_FINISHED, YES);
                }
            }

            values.put(SAVE_DATA_COL, s);
            values.put(SAVE_ICON, imgBytes);
            values.put(SAVED_COL, YES);
            values.put(SAVE_X, x);
            values.put(SAVE_Y, y);
            values.put(SAVE_R, revision);
            values.put(SAVE_ASSISTED, (computerAssist) ? YES : NO);

            db.update(PUZZLES_TABLE, values, PUZZLE_NAME_COL + "=?", new String[]{puzzleName});
        }

        // update this puzzle as the last saved puzzle in the preferences
        SudokuUtils.setLastSavePreference(mContext, puzzleName);

        return true;
    }

    public int[] getRandomPuzzle(String category) {
        synchronized (ThisBackupAgent.sSyncLock) {
            Cursor cursor;
            SQLiteDatabase db = getReadableDatabase();

            if (category.equals(mContext.getString(R.string.any_category))) {
                cursor = db.query(PUZZLES_TABLE,
                        new String[]{PUZZLE_DATA_COL},
                        null, null, null, null, null);
            } else {
                cursor = db.query(PUZZLES_TABLE,
                        new String[]{PUZZLE_DATA_COL},
                        PUZZLES_CATEGORY_COL + "=?",
                        new String[]{category},
                        null, null, null);
            }

            int index;
            byte[] rawData;
            int[] puzzle = null;
            int size = cursor.getCount();
            if (size > 0) {
                index = new Random().nextInt(size);
                Log.i("PUZZLEMANAGER", "Returning random puzzle at index: " + index + " of: " + size + " for category: " + category);
                cursor.moveToPosition(index);
                rawData = cursor.getBlob(cursor.getColumnIndex(PUZZLE_DATA_COL));
                puzzle = new int[rawData.length];
                for (int i = 0; i < rawData.length; i++) puzzle[i] = (int) rawData[i];
            }

            cursor.close();
            return puzzle;
        }
    }

    public void saveNewPuzzle(Bitmap bitmap, String name, String description, Cell[] template,
                              String category, boolean fromImage) {
        int[] templateInt = new int[template.length];
        for (int i = 0; i < template.length; i++) {
            if (template[i].isLocked()) {
                templateInt[i] = template[i].getValue();
            } else {
                templateInt[i] = Cell.NONE;
            }
        }
        saveNewPuzzle(bitmap, name, description, templateInt, category, fromImage);
    }

    public void saveNewPuzzle(Bitmap bitmap, String name, String description, int[] template,
                              String category, boolean fromImage) {

        if (template == null) return;

        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = this.getWritableDatabase();

            if (containsPuzzle(db, name)) {
                updatePuzzle(bitmap, name, template);
                return;
            }

            byte[] templateBlob = new byte[template.length];
            for (int i = 0; i < templateBlob.length; i++)
                templateBlob[i] = (byte) template[i];


            byte[] imgBytes = SudokuUtils.bitmapToBytes(bitmap);


            // insert basic puzzle data
            ContentValues values = new ContentValues();
            values.put(PUZZLE_NAME_COL, name);
            values.put(PUZZLE_DESC_COL, description);
            values.put(PUZZLE_DATA_COL, templateBlob);
            values.put(PUZZLE_ICON_COL, imgBytes);
            values.put(PUZZLES_CATEGORY_COL, category);
            db.insert(PUZZLES_TABLE, null, values);

            // basic stats
            values = new ContentValues();
            values.put(PUZZLE_NAME_COL, name);      // links the stats record to the puzzle record
            values.put(TIMES_OPENED, 1);            // marks this as the first "open"
            int fi = (fromImage) ? YES : NO;        // whether or not this was created from an image, non-null applies to PUZZLES_ADDED
            values.put(FROM_IMAGE, fi);             // ^
            db.insert(STATS_TABLE, null, values);
        }
    }

    public void updatePuzzle(Bitmap bitmap, String name, int[] template) {

        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = this.getWritableDatabase();

            byte[] templateBlob = new byte[template.length];
            for (int i = 0; i < templateBlob.length; i++)
                templateBlob[i] = (byte) template[i];

            String newName = name;
            int i = 1;
            while (containsPuzzle(db, newName)) {
                newName = name + " (" + i + ")";
                i++;
            }

            byte[] imgBytes = SudokuUtils.bitmapToBytes(bitmap);
            bitmap.recycle();

            ContentValues values = new ContentValues();
            values.put(PUZZLE_NAME_COL, name);
            values.put(PUZZLE_DATA_COL, templateBlob);
            values.put(PUZZLE_ICON_COL, imgBytes);
            db.update(PUZZLES_TABLE, values, PUZZLE_NAME_COL + "=?", new String[]{name});
        }
    }

    @Deprecated
    public boolean containsPuzzle(String templateName) {
        SQLiteDatabase db = getReadableDatabase();
        return containsPuzzle(db, templateName);
    }

    public boolean containsPuzzle(SQLiteDatabase db, String templateName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            if (templateName == null) return false;
            Cursor cursor = db.query(PUZZLES_TABLE, new String[]{PUZZLE_NAME_COL}, PUZZLE_NAME_COL + "=?", new String[]{templateName}, null, null, null);
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            return exists;
        }
    }

    public boolean containsSave(String templateName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            if (templateName == null) return false;
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(PUZZLES_TABLE,
                    new String[]{SAVED_COL},
                    PUZZLE_NAME_COL + "=?",
                    new String[]{templateName},
                    null,
                    null,
                    null);
            boolean exists = cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndex(SAVED_COL)) == YES;
            cursor.close();
            return exists;
        }
    }

    public String nextAvailableName(String desiredName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            int n = 1;
            String newName = desiredName;
            while (containsPuzzle(db, newName)) {
                Log.i("PuzzleManager", "Next available name, cant be " + newName);
                newName = desiredName + " (" + n + ")";
                n++;
            }
            return newName;
        }
    }

    public void deleteSaveState(String templateName) {

        synchronized (ThisBackupAgent.sSyncLock) {
            // since this puzzle is being deleted, save all of the stats to the statistics table so we don't lose them
            SQLiteDatabase db = getWritableDatabase();

            ContentValues values = new ContentValues();
            //these columns shouldn't be null
            values.put(SAVE_DURATION, 0);
            // these columns can be null
            values.putNull(SAVED_COL);
            values.putNull(SAVE_FINISHED);
            values.putNull(SAVE_ICON);
            values.putNull(SAVE_DATA_COL);
            values.putNull(SAVE_X);
            values.putNull(SAVE_Y);
            values.putNull(SAVE_R);
            values.putNull(SAVE_ASSISTED);

            db.update(PUZZLES_TABLE,
                    values,
                    PUZZLE_NAME_COL + "=?",
                    new String[]{templateName});
        }
    }

    public void wipeStatistics() {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();

            String sql = String.format("DELETE FROM %s WHERE %s is null", STATS_TABLE, PUZZLE_NAME_COL);
            db.execSQL(sql);

            ContentValues wipeValues = new ContentValues();
            wipeValues.putNull(FROM_IMAGE);
            wipeValues.put(BEST_FINISH_TIME_UNASSISTED, 0);
            wipeValues.put(BEST_FINISH_TIME_ASSISTED, 0);
            wipeValues.put(LONGEST_SESSION, 0);
            wipeValues.put(FINISH_COUNT, 0);
            wipeValues.put(COMPUTER_ASSIST_COUNT, 0);
            wipeValues.put(TIMES_OPENED, 0);
            wipeValues.put(TOTAL_DURATION, 0);
            db.update(STATS_TABLE, wipeValues, null, null);
        }
    }

    public void deletePuzzle(String puzzle) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();

            String sql = String.format("DELETE FROM %s WHERE %s = ?", PUZZLES_TABLE, PUZZLE_NAME_COL);
            db.execSQL(sql, new String[]{puzzle});

            ContentValues values = new ContentValues();
            values.putNull(PUZZLE_NAME_COL);
            db.update(STATS_TABLE, values, PUZZLE_NAME_COL + "=?", new String[]{puzzle});
        }
    }

    public void logFinishTime(String puzzleName, long duration, boolean computerAssisted) {
        if (computerAssisted) {
            updateStat(puzzleName, BEST_FINISH_TIME_ASSISTED, true, duration);
        } else {
            updateStat(puzzleName, BEST_FINISH_TIME_UNASSISTED, true, duration);
        }
    }

    private void updateStat(String puzzleName, String columnName, boolean updateIfSmaller, long n) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();

            String statement = (updateIfSmaller) ? "UPDATE %s SET %s=? WHERE (%s>? OR %s=0 OR %s is null) AND %s=?" :
                    "UPDATE %s SET %s=? WHERE (%s<? OR %s=0 OR %s is null) AND %s=?";

            String sql = String.format(statement,
                    STATS_TABLE,
                    columnName,
                    columnName,
                    columnName,
                    columnName,
                    PUZZLE_NAME_COL);

            db.execSQL(sql, new Object[]{n, n, puzzleName});
        }
    }

    public void renamePuzzle(String oldName, String newName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(PUZZLE_NAME_COL, newName);
            db.update(PUZZLES_TABLE, cv, PUZZLE_NAME_COL + "=?", new String[]{oldName});
            db.update(STATS_TABLE, cv, PUZZLE_NAME_COL + "=?", new String[]{oldName});
        }

        // this handles updating the last puzzle save for the home screen resume button
        String lastSavePuzzle = SudokuUtils.getLastSavePreference(mContext);
        if (lastSavePuzzle != null && lastSavePuzzle.equals(oldName)) {
            SudokuUtils.setLastSavePreference(mContext, newName);
        }
    }

    public void close() {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db.isOpen()) db.close();
            Log.i("PUZZLEMANAGER", "Closed");
        }
    }

    public boolean restoreDefaultPuzzles() {
        synchronized (ThisBackupAgent.sSyncLock) {
            File tempDatabaseFile = getDatabaseAssetFile();
            if (tempDatabaseFile != null) {
                SQLiteDatabase toDB = getWritableDatabase();
                String attachStatement = "ATTACH ? AS defaultdatabase";
                String insertStatementPUZZLES = String.format("INSERT OR IGNORE INTO main.%s SELECT * FROM defaultdatabase.%s", PUZZLES_TABLE, PUZZLES_TABLE);
                String insertStatementSTATS = String.format("INSERT OR IGNORE INTO main.%s SELECT * FROM defaultdatabase.%s", STATS_TABLE, STATS_TABLE);
                String detachStatement = "DETACH defaultdatabase";
                toDB.execSQL(attachStatement, new String[]{tempDatabaseFile.getAbsolutePath()});
                toDB.execSQL(insertStatementPUZZLES);
                toDB.execSQL(insertStatementSTATS);
                toDB.execSQL(detachStatement);
                if (tempDatabaseFile.delete()) {
                    Log.i("PuzzleManager", "Temporary database file deleted");
                } else {
                    Log.e("PuzzleManager", "Failed to delete temporary database file");
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private File getDatabaseAssetFile() {
        InputStream is = null;
        FileOutputStream fos = null;
        File tempFile = null;

        try {
            is = mContext.getAssets().open("databases" + File.separator + DB_NAME);
            tempFile = SudokuUtils.createTemporaryFile(mContext);

            fos = new FileOutputStream(tempFile);
            int read;
            byte[] buffer = new byte[1024];
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }

            fos.close();
            is.close();

        } catch (IOException e) {
            Log.e("PuzzleManager", "Failed to load database from assets");
            e.printStackTrace();
        }

        return tempFile;
    }

    public Bundle getSaveState(@NonNull String puzzleName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.query(PUZZLES_TABLE,
                    null,
                    PUZZLE_NAME_COL + "=?",
                    new String[]{puzzleName},
                    null,
                    null,
                    null);

            Bundle bundle = null;
            if (cursor.moveToFirst()) {
                bundle = new Bundle();
                bundle.putInt(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_SAVE_STATE);
                bundle.putString(PuzzleActivity.INTENT_PUZZLE_NAME, cursor.getString(cursor.getColumnIndex(PUZZLE_NAME_COL)));
                bundle.putString(PuzzleActivity.INTENT_PUZZLE_DATA, cursor.getString(cursor.getColumnIndex(SAVE_DATA_COL)));
                bundle.putInt(PuzzleActivity.INTENT_SAVE_X, cursor.getInt(cursor.getColumnIndex(SAVE_X)));
                bundle.putInt(PuzzleActivity.INTENT_SAVE_Y, cursor.getInt(cursor.getColumnIndex(SAVE_Y)));
                bundle.putLong(PuzzleActivity.INTENT_PUZZLE_DURATION, cursor.getLong(cursor.getColumnIndex(SAVE_DURATION)));
                bundle.putShort(PuzzleActivity.INTENT_SAVE_R, cursor.getShort(cursor.getColumnIndex(SAVE_R)));
                bundle.putBoolean(PuzzleActivity.INTENT_SAVE_ASSISTED, cursor.getInt(cursor.getColumnIndex(SAVE_ASSISTED)) == YES);
            }

            cursor.close();

            return bundle;
        }
    }

    private long getTotalStat(String puzzleTableColumn) {
        return getStatFromFunction(puzzleTableColumn, "TOTAL");
    }

    private long getLowestStat(String puzzleTableColumn) {
        return getStatFromFunction(puzzleTableColumn, "MIN");
    }

    private long getHighestStat(String puzzleTableColumn) {
        return getStatFromFunction(puzzleTableColumn, "MAX");
    }

    private long getStatFromFunction(String puzzleTableColumn, String function) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            long stat = 0;

            if (puzzleTableColumn != null) {
                String sql = String.format("SELECT %s(%s) FROM (SELECT %s FROM %s WHERE %s != 0)",
                        function, puzzleTableColumn, puzzleTableColumn, STATS_TABLE, puzzleTableColumn);
                Cursor puzzleCursor = db.rawQuery(sql, null);
                if (puzzleCursor.moveToFirst() && !puzzleCursor.isNull(0))
                    stat = puzzleCursor.getLong(0);
                puzzleCursor.close();
            }

            return stat;
        }
    }

    public long getTotalPlayTime() {
        return getTotalStat(TOTAL_DURATION);
    }

    public int getTotalPlayCount() {
        return (int) getTotalStat(TIMES_OPENED);
    }

    public int getTotalAssistCount() {
        return (int) getTotalStat(COMPUTER_ASSIST_COUNT);
    }

    public int getTotalFinishCount() {
        return (int) getTotalStat(FINISH_COUNT);
    }

    public int getNumberOfPuzzlesAdded(boolean fromImage) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            String sql;
            if (fromImage) {
                sql = String.format("SELECT %s FROM %s WHERE %s = %s",
                        FROM_IMAGE, STATS_TABLE, FROM_IMAGE, YES);
            } else {
                sql = String.format("SELECT %s FROM %s WHERE %s IS NOT NULL",
                        FROM_IMAGE, STATS_TABLE, FROM_IMAGE);
            }
            Cursor cursor = db.rawQuery(sql, null);
            int n = cursor.getCount();
            cursor.close();

            return n;
        }
    }

    public int getNumberOfPuzzlesDeleted() {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            String sql = String.format("SELECT %s FROM %s WHERE %s IS NULL",
                    PUZZLE_NAME_COL, STATS_TABLE, PUZZLE_NAME_COL);
            Cursor cursor = db.rawQuery(sql, null);
            int n = cursor.getCount();
            cursor.close();
            return n;
        }
    }

    public String getMostPlayedPuzzle() {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            String name = null;

            String sql = String.format("SELECT %s, %s FROM %s WHERE %s = (SELECT MAX(%s) FROM %s)",
                    PUZZLE_NAME_COL, TOTAL_DURATION, STATS_TABLE, TOTAL_DURATION, TOTAL_DURATION, STATS_TABLE);

            Cursor cursor = db.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                long count = cursor.getLong(cursor.getColumnIndex(TOTAL_DURATION));
                if (count > 0) name = cursor.getString(cursor.getColumnIndex(PUZZLE_NAME_COL));
            }
            cursor.close();
            return name;
        }
    }

    public long getLongestPuzzleDuration() {
        return getHighestStat(TOTAL_DURATION);
    }

    public String getBestFinishPuzzleName(boolean computerAssisted) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            String name = null;

            String columnName = (computerAssisted) ? BEST_FINISH_TIME_ASSISTED : BEST_FINISH_TIME_UNASSISTED;

            String sql = String.format("SELECT %s, %s FROM %s WHERE %s = (SELECT MIN(%s) from (SELECT %s FROM %s WHERE %s != 0))",
                    PUZZLE_NAME_COL, columnName, STATS_TABLE, columnName, columnName, columnName, STATS_TABLE, columnName);

            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                long finishTime = cursor.getLong(cursor.getColumnIndex(columnName));
                if (finishTime != 0)
                    name = cursor.getString(cursor.getColumnIndex(PUZZLE_NAME_COL));
            }
            cursor.close();

            return name;
        }
    }

    public long getBestFinishTime(boolean computerAssisted) {
        return getLowestStat((computerAssisted) ? BEST_FINISH_TIME_ASSISTED : BEST_FINISH_TIME_UNASSISTED);
    }

    public long getLongestSession() {
        return getHighestStat(LONGEST_SESSION);
    }

    public long getSingleStat(final String puzzleName, final String columnName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(
                    STATS_TABLE,
                    new String[]{columnName},
                    PUZZLE_NAME_COL + "=?",
                    new String[]{puzzleName},
                    null, null, null);
            long value = 0;
            if (cursor.moveToFirst()) {
                value = cursor.getLong(cursor.getColumnIndex(columnName));
            }
            cursor.close();

            return value;
        }
    }

    public int getPuzzleDifficulty(String puzzleName) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getReadableDatabase();
            int difficulty = -1;
            Cursor cursor = db.query(PUZZLES_TABLE, new String[]{PUZZLE_ESTIMATED_DIFFICULTY}, PUZZLE_NAME_COL + "=?", new String[]{puzzleName},
                    null, null, null, null);

            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(PUZZLE_ESTIMATED_DIFFICULTY);
                if (!cursor.isNull(index)) {
                    difficulty = cursor.getInt(index);
                }
            }
            cursor.close();

            return difficulty;
        }
    }

    public int getTimesFinished(String puzzleName) {
        return (int) getSingleStat(puzzleName, PuzzleManager.FINISH_COUNT);
    }

    public int getNewGamesStarted(String puzzleName) {
        return (int) getSingleStat(puzzleName, PuzzleManager.TIMES_OPENED);
    }

    public long getTimePlayed(String puzzleName) {
        return getSingleStat(puzzleName, PuzzleManager.TOTAL_DURATION);
    }

    public synchronized void setPuzzleDifficulty(String puzzleName, int difficulty) {
        synchronized (ThisBackupAgent.sSyncLock) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(PUZZLE_ESTIMATED_DIFFICULTY, difficulty);
            db.update(PUZZLES_TABLE, values, PUZZLE_NAME_COL + "=?", new String[]{puzzleName});
        }
    }
}
