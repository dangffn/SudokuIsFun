package com.danbuntu.androidsudoku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Dan on 5/16/2016.
 */
public class TemplateManager extends SQLiteOpenHelper {

    final static int DB_VERSION = 11;

    final static String DB_NAME = "sudoku";
    final static String TEMPLATES_TABLE = "templates";

    final static String NAME_COL = "_id";
    final static String DESC_COL = "description";
    final static String TEMPLATE_DATA_COL = "template_data";
    final static String TEMPLATE_ICON = "template_icon";
    final static String SAVED_COL = "saved";
    final static String SAVE_ICON = "save_icon";
    final static String SAVE_DATA_COL = "save_data";
    final static String SAVE_X = "save_x";
    final static String SAVE_Y = "save_y";

    final static int HAS_SAVE=1;
    final static int NO_SAVE=0;

    public TemplateManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TEMPLATES_TABLE + "(" + NAME_COL + " TEXT COLLATE NOCASE, " + DESC_COL + " TEXT, " + TEMPLATE_DATA_COL + " BLOB, " + TEMPLATE_ICON + " BLOB, " + SAVE_ICON + " BLOB, " + SAVE_DATA_COL + " TEXT, " + SAVED_COL + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS " + TEMPLATES_TABLE);
        db.execSQL("ALTER TABLE " + TEMPLATES_TABLE + " ADD COLUMN " + SAVE_X + " INTEGER");
        db.execSQL("ALTER TABLE " + TEMPLATES_TABLE + " ADD COLUMN " + SAVE_Y + " INTEGER");
//        onCreate(db);
    }

    public String[] getNames(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + NAME_COL + "," + DESC_COL + " FROM " + tableName, null);

        String[] names = new String[cursor.getCount()];
        if (cursor.moveToFirst()) {
            do {
                names[cursor.getPosition()] = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return names;
    }

    public int[] getTemplate(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + TEMPLATE_DATA_COL + " FROM " + TEMPLATES_TABLE + " WHERE " + NAME_COL + " = ?", new String[]{name});
        byte[] raw = null;
        if (cursor.moveToFirst()) raw = cursor.getBlob(0);
        cursor.close();
        db.close();

        int[] template = new int[raw.length];
        for (int i = 0; i < template.length; i++)
            template[i] = (int)raw[i];

        return template;
    }

    public ArrayList<String> getSaveNames() {
        ArrayList<String> names = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TEMPLATES_TABLE, new String[]{ NAME_COL }, SAVE_DATA_COL + " IS NOT NULL and " + SAVE_DATA_COL + " IS NOT ?", new String[]{""}, null, null, null);
        if(cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(0));
            } while(cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return names;
    }

    public String getSave(String templateName) {
        String result = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TEMPLATES_TABLE, new String[]{ SAVE_DATA_COL }, NAME_COL + "=?", new String[]{ templateName }, null, null, null);
        if(cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        db.close();

        return result;
    }

    public void removeTemplates(String name) {
        removeTemplate(new String[]{ name });
    }

    public void removeTemplate(String[] names) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TEMPLATES_TABLE, NAME_COL + "=?", names);
        db.close();
    }

    public boolean saveState(Bitmap bitmap, String name, Cell[] cells, int x, int y) {
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

        byte[] imgBytes = null;
        if(bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imgBytes = stream.toByteArray();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(SAVE_DATA_COL, s);
        vals.put(SAVE_ICON, imgBytes);
        vals.put(SAVED_COL, HAS_SAVE);
        vals.put(SAVE_X, x);
        vals.put(SAVE_Y, y);
        db.update(TEMPLATES_TABLE, vals, NAME_COL + "=?", new String[]{ name });
        db.close();

        return true;
    }

    public void saveTemplate(Bitmap bitmap, String name, String description, Cell[] template) {

        if(containsTemplateName(name)) {
            updateTemplate(bitmap, name, template);
            return;
        }

        byte[] templateBlob = new byte[template.length];
        for (int i = 0; i < templateBlob.length; i++)
            templateBlob[i] = (byte)template[i].getValue();


        byte[] imgBytes = null;
        if(bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imgBytes = stream.toByteArray();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(NAME_COL, name);
        vals.put(DESC_COL, description);
        vals.put(TEMPLATE_DATA_COL, templateBlob);
        vals.put(TEMPLATE_ICON, imgBytes);
        db.insert(TEMPLATES_TABLE, null, vals);
        db.close();

    }

    public void updateTemplate(Bitmap bitmap, String name, Cell[] template) {

        byte[] templateBlob = new byte[template.length];
        for (int i = 0; i < templateBlob.length; i++)
            templateBlob[i] = (byte)template[i].getValue();

        String newName = name;
        int i=1;
        while(containsTemplateName(newName)) {
            newName = name + " (" + i + ")";
            i++;
        }

        byte[] imgBytes = null;
        if(bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imgBytes = stream.toByteArray();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(NAME_COL, name);
        vals.put(TEMPLATE_DATA_COL, templateBlob);
        vals.put(TEMPLATE_ICON, imgBytes);
        db.update(TEMPLATES_TABLE, vals, NAME_COL + "=?", new String[]{ name });
        db.close();

    }

    public boolean containsTemplateName(String templateName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TEMPLATES_TABLE, new String[]{NAME_COL}, NAME_COL + "=?", new String[]{templateName}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public boolean containsSave(String templateName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TEMPLATES_TABLE, new String[]{NAME_COL}, NAME_COL + "=? AND " + SAVED_COL + "=?", new String[]{templateName, String.valueOf(HAS_SAVE)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
}
