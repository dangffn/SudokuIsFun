package com.danbuntu.sudokuisfun;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.danbuntu.sudokuisfun.puzzle.Puzzle;
import com.danbuntu.sudokuisfun.ui.PuzzleManager;

import java.util.ArrayList;

/**
 * Reformat and clean data for release
 */
public class ProductionReadinessTest extends ApplicationTestCase<Application> {

    public ProductionReadinessTest() {
        super(Application.class);
    }

    public void testDeleteNonDefaultData() {
        PuzzleManager pm = PuzzleManager.getInstance(getContext());
        SQLiteDatabase db = pm.getWritableDatabase();

        String sql1 = "UPDATE puzzles SET saved=NULL, save_duration=0, save_data=null, save_icon=null, save_x=null, save_y=null, save_r=null, save_finished=null, save_assisted=null";
        String sql2 = "DELETE FROM stats";
        String sql3 = "INSERT INTO stats (_id) SELECT _id FROM puzzles";

        db.execSQL(sql1);
        db.execSQL(sql2);
        db.execSQL(sql3);

        pm.close();
    }
}