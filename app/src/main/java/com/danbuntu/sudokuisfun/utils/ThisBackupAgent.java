package com.danbuntu.sudokuisfun.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.danbuntu.sudokuisfun.ui.PuzzleManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by dan on 8/3/16. Have a great day!
 */
public class ThisBackupAgent extends BackupAgentHelper {

    final String SHARED_PREF_HELPER_BACKUP_KEY = "preferences";
    final String FILE_HELPER_BACKUP_KEY = "files";
    public static final Object sSyncLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();

        // takes care of preferences
        String sharedPrefName = getPackageName() + "_preferences";
        SharedPreferencesBackupHelper spHelper =
                new SharedPreferencesBackupHelper(this, sharedPrefName);
        addHelper(SHARED_PREF_HELPER_BACKUP_KEY, spHelper);

        // I hate this
        // takes care of database
        String databaseDirName = getDatabasePath(PuzzleManager.DB_NAME).getParentFile().getName();
        String databaseFile = ".." + File.separator + databaseDirName + File.separator + PuzzleManager.DB_NAME;

        // takes care of ocrdata-trained
        File ocrDataFile = SudokuUtils.getExternalOCRFile(this);
        String externalOcrFile = ocrDataFile.getName();

        Log.i("ThisBackupAgent", "Database location recorded as: " + databaseFile);
        Log.i("ThisBackupAgent", "OCRData location recorded as: " + externalOcrFile);

        // wrap it all up
        FileBackupHelper fileHelper =
                new FileBackupHelper(this,
                        databaseFile,
                        externalOcrFile);
        addHelper(FILE_HELPER_BACKUP_KEY, fileHelper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        synchronized(ThisBackupAgent.sSyncLock) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        synchronized(ThisBackupAgent.sSyncLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }
}
