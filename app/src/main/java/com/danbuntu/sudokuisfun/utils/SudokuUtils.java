package com.danbuntu.sudokuisfun.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.puzzle.Puzzle;
import com.danbuntu.sudokuisfun.ui.PuzzleManager;
import com.danbuntu.sudokuisfun.ui.Statistic;
import com.danbuntu.sudokuisfun.ui.StatisticsAdapter;
import com.danbuntu.sudokuisfun.ui.SudokuGridView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dan on 7/1/2016. Have a great day!
 */
public class SudokuUtils {

    public final static int READ_STORAGE_REQUEST = 1;
    public final static int WRITE_STORAGE_REQUEST = 2;
    public final static int CAMERA_PERMISSION_REQUEST = 3;
    public final static int INTERNET_PERMISSION_REQUEST = 4;

    final static String TEMP_FILE_NAME = "sudoku";
    final static String TAG = "SudokuUtils";

    public static void setLastSavePreference(Context context, String puzzleName) {
        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefsEditor.putString(context.getString(R.string.pref_key_lastPuzzle), puzzleName);
        prefsEditor.apply();
    }

    public static String getLastSavePreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_key_lastPuzzle), null);
    }

    public static boolean checkWritePermissions(Context context) {
        int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writePermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to write external storage has been granted by default");
            return true;
        } else {
            Log.d(TAG, "Permission to write external storage has not been granted, requesting permission");
            return false;
        }
    }

    public static void requestWritePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_STORAGE_REQUEST);
    }

    public static boolean checkCameraPermissions(Context context) {
        int cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to the camera has been granted by default");
            return true;
        } else {
            Log.d(TAG, "Permissions to the camera have not been granted, requesting permission");
            return false;
        }
    }

    public static boolean checkInternetPermissions(Context context) {
        int internetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET);
        if (internetPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to the internet has been granted by default");
            return true;
        } else {
            Log.d(TAG, "Permission to the internet has not been granted, requesting permission");
            return false;
        }
    }

    public static void requestInternetPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.INTERNET},
                INTERNET_PERMISSION_REQUEST);
    }

    public static void requestCameraPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    public static boolean checkReadPermissions(Context context) {
        int readPermission;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            // according to developer.android.com apis below 19 auto allow READ_EXTERNAL_STORAGE
            // <a href="https://developer.android.com/reference/android/Manifest.permission.html#READ_EXTERNAL_STORAGE">Click Here</a>
            Log.d(TAG, "API is below 19, external read permissions are allowed by default");
            return true;
        }

        if (readPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "External read permission has been granted by default");
            return true;
        } else {
            Log.d(TAG, "External read permission has not been granted, requesting permission");
            return false;
        }
    }

    public static void requestReadPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_REQUEST);
        }
    }

    public static void showInfoDialog(Context context, final PuzzleManager pm, final String puzzleName) {
        if (puzzleName == null) return;

        LinearLayout dialogView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_puzzle_info, null);
        ListView listView = (ListView) dialogView.findViewById(R.id.lstPuzzleStats);
        ImageView dialogImage = (ImageView) dialogView.findViewById(R.id.imgPuzzleStats);
        TextView txtPuzzleTitle = (TextView) dialogView.findViewById(R.id.txtPuzzleTitle);
        dialogView.removeView(txtPuzzleTitle);

        Bitmap icon = pm.getPuzzleIcon(puzzleName);
        if (icon == null) {
            dialogImage.setVisibility(View.GONE);
        } else {
            dialogImage.setImageBitmap(icon);
        }

        final StatisticsAdapter adapter = new StatisticsAdapter(context, R.layout.listitem_stats, false);

        final Statistic difficultyStat = new Statistic("Estimated Difficulty", null, null);

        final Puzzle puzzle = new Puzzle();

        int difficulty = pm.getPuzzleDifficulty(puzzleName);
        if (difficulty == -1) {
            int[] puzzleData = pm.getPuzzleData(puzzleName, false);
            puzzle.loadPuzzleData(puzzleData);

            difficultyStat.setValue("Calculating...");
            adapter.notifyDataSetChanged();

            puzzle.setPuzzleListener(new Puzzle.PuzzleListener() {
                @Override
                public void puzzleSolveStarted() {
                }

                @Override
                public void puzzleSolveEnded() {
                }

                @Override
                public void puzzleSolved() {

                    final int d = puzzle.getDifficulty(1, 10);
                    difficultyStat.setValue(String.format("%s / 10", d));
                    adapter.notifyDataSetChanged();

                    // update the difficulty in the database so we don't have to re-run this
                    // this should be pre-run for all packaged puzzles
                    pm.setPuzzleDifficulty(puzzleName, d);
                }

                @Override
                public void puzzleResumed() {
                }

                @Override
                public void puzzleImpossible() {
                    difficultyStat.setValue("Impossible");
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void puzzleError() {
                    difficultyStat.setValue("Error in puzzle");
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void undoNotAvailable() {
                }

                @Override
                public void undoAvailable() {
                }
            });

            puzzle.doSolve(true);
        } else {
            difficultyStat.setValue(difficulty + " / 10");
        }


        ArrayList<Statistic> statItems = new ArrayList<>();


        statItems.add(new Statistic("Puzzle Name", puzzleName, null));
        statItems.add(
                new Statistic("Total Time Played",
                        SudokuUtils.getReadableDuration(pm.getTimePlayed(puzzleName)), null));
        statItems.add(
                new Statistic("New Games Started",
                        SudokuUtils.formatTousandths(pm.getNewGamesStarted(puzzleName)), null));
        statItems.add(
                new Statistic("Times Finished",
                        SudokuUtils.formatTousandths(pm.getTimesFinished(puzzleName)), null));
        statItems.add(difficultyStat);

        adapter.addAll(statItems);
        listView.setAdapter(adapter);

        new AlertDialog.Builder(context)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        puzzle.stopSolving();
                    }
                })
                .setView(dialogView)
                .setCustomTitle(txtPuzzleTitle)
                .create().show();
    }

    @Deprecated
    public static Bitmap resolveBitmap(Intent intent, Context context) {
        //TODO this should run in a separate thread
        return resolveBitmap(intent, context, -1);
    }

    public static Bitmap resolveBitmap(Intent intent, Context context, int maxSize) {
        String path = getPathFromIntent(intent, context);

        Bitmap bitmap;
        if (maxSize == -1) {
            bitmap = BitmapFactory.decodeFile(path);
        } else {

            // reduce the size of the bitmap below the requested maxSize
            int sampleSize = 1;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            int w = opts.outWidth;
            int h = opts.outHeight;

            if (w > maxSize || h > maxSize) {
                sampleSize++;
                w /= 2;
                h /= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;

            bitmap = BitmapFactory.decodeFile(path, opts);
        }

        return bitmap;
    }

    private static String getPathFromIntent(Intent intent, Context context) {
        Uri uri;
        String path;

        if ((uri = intent.getData()) == null) uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if ((path = queryPathInUri(uri, context)) == null) {
            path = createTempFileFromUriStream(uri, context);
        }

        return path;
    }

    private static String queryPathInUri(Uri uri, Context context) {
        if (uri == null) return null;

        String path = null;

        if (MediaStore.AUTHORITY.equals(uri.getAuthority())) {

            // find image in the ContentResolver query
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor cursor;
            cursor = context.getContentResolver().query(uri, filePath, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();
                path = cursor.getString(cursor.getColumnIndex(filePath[0]));
                cursor.close();
            }
        }
        return path;
    }

    private static String createTempFileFromUriStream(Uri uri, Context context) {
        String path = null;
        InputStream is;

        try {

            // found this gem on the internet, this should handle Picassa image selections from the gallery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                if (parcelFileDescriptor == null) return null;
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                is = new FileInputStream(fileDescriptor);
            } else {
                is = context.getContentResolver().openInputStream(uri);
            }

            File tempFile = createTemporaryFile(context);
            FileOutputStream fos = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            if (is == null) return null;
            int len = is.read(buffer);
            while (len != -1) {
                fos.write(buffer, 0, len);
                len = is.read(buffer);
            }
            is.close();
            fos.flush();
            fos.close();
            path = tempFile.getAbsolutePath();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    public static Object[][] rotateArray(Object[][] array, boolean clockWise) {
        if (array == null || array.length <= 0) return null;
        Object[][] outArray = new Object[array[0].length][array.length];

        int inverseY = array.length - 1;
        for (int y = 0; y < array.length; y++) {

            int inverseX = array[0].length - 1;
            for (int x = 0; x < array[0].length; x++) {

                if (clockWise) {
                    outArray[x][inverseY] = array[y][x];
                } else {
                    outArray[x][y] = array[y][inverseX];
                }
                inverseX--;
            }

            inverseY--;
        }

        return outArray;
    }

    public static Object[][] shuffleArray(Object[][] array) {
        if (array == null || array.length <= 0) return null;

        List<Integer> blockIndices = Arrays.asList(0, 1, 2);
        List<Integer> rowIndices = Arrays.asList(0, 1, 2);

        int blockSize = blockIndices.size();
        int inRow = 0;

        // block shuffle once
        Collections.shuffle(blockIndices);

        Object[][] outArray = new Object[array.length][array[0].length];
        for (int blockIndex : blockIndices) {

            // row shuffle on each block iteration
            Collections.shuffle(rowIndices);

            int blockStart = blockSize * blockIndex;
            for (int rowIndex : rowIndices) {
                outArray[blockStart + rowIndex] = array[inRow];
                inRow++;
            }
        }

        return outArray;
    }

    public static String getDate() {
        Calendar cal = Calendar.getInstance();
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("MM-dd-yy", Locale.US);
        return date.format(currentLocalTime);
    }

    public static File createTemporaryFile(Context context) throws IOException {
        File tempDir = context.getCacheDir();

        long ts = System.currentTimeMillis();
        return File.createTempFile(TEMP_FILE_NAME + "-" + ts, ".dat", tempDir);
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        byte[] imgBytes = null;
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imgBytes = stream.toByteArray();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return imgBytes;
    }

    public static void clearAppCache(Context context) {
        boolean clean = true;
        File cacheDir = context.getCacheDir();
        if (cacheDir.exists()) clean = clearFolderContents(cacheDir, 3);

        if (clean) {
            Log.i(TAG, "Cleared all cache files successfully");
        } else {
            Log.e(TAG, "Wasn't able to clear all cache files");
        }
    }

    private static boolean clearFolderContents(File folder, int depthToGo) {
        if (folder == null || !folder.isDirectory() || depthToGo < 0) return false;

        boolean success = true;

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                if (!file.delete()) success = false;
            } else if (file.isDirectory()) {
                if (!clearFolderContents(file, depthToGo - 1)) success = false;
            } else {
                // dunno what it is at this point, shouldn't ever get here
                success = false;
            }
        }

        return success;
    }

    public static String formatTousandths(int integer) {
        return NumberFormat.getIntegerInstance().format(integer);
    }

    public static String getReadableDuration(long duration) {
        int hours = (int) TimeUnit.MILLISECONDS.toHours(duration);
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
    }

    public static int[] puzzleFromString(String input) {
        if (input.length() != 81) {
            Log.e("SudokuUtils", "Attempted to load puzzle from string with length of: " + input.length());
            return null;
        }

        int[] puzzle = new int[input.length()];
        Arrays.fill(puzzle, -1);
        try {
            for (int i = 0; i < input.length(); i++) {
                int n = Character.getNumericValue(input.charAt(i));
                if (n >= 1 || n <= 9) {
                    puzzle[i] = n;
                }
            }
            return puzzle;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String repeat(int count, String with) {
        return new String(new char[count]).replace("\0", with);
    }

    public static String sFill(int count, String string) {
        return repeat(count - string.length(), " ") + string;
    }

    public static int[] createNewPuzzle(Context context, PuzzleManager pm, String name, int[] puzzleData, String category, boolean scramble, boolean saveIcon) {

        SudokuGridView gridView = new SudokuGridView(context);
        Puzzle puzzle = new Puzzle(gridView);
        puzzle.loadPuzzleData(puzzleData);

        if (scramble) {
            puzzle.obfuscatePuzzle();
            puzzleData = puzzle.getPuzzleData();
        }

        Bitmap icon = null;
        if (saveIcon) {
            int iconSize = context.getResources().getInteger(R.integer.icon_size);
            icon = gridView.getSnapshot(iconSize, iconSize, iconSize, false);
        }

        String description = String.format(context.getString(R.string.created_on), SudokuUtils.getDate());
        pm.saveNewPuzzle(icon, name, description, puzzleData, category, false);

        return puzzleData;
    }

}
