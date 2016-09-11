package com.danbuntu.androidsudoku;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Dan on 5/12/2016.
 */
public class GalleryGrab {

    Context context;
    Intent intent;

    public GalleryGrab(Intent intent, Context context) {
        this.context = context;
        this.intent = intent;
    }

    public Bitmap resolveBitmap() {
        String path = getPathFromIntent(intent, context);

        return BitmapFactory.decodeFile(path);
    }

    private String getPathFromIntent(Intent intent, Context context) {

        String path;

        path = queryPathInUri(intent.getData());
        if (path == null)
            path = queryPathInUri((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        if (path == null)
            path = createTempFileFromUriStream((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        if (path == null) path = createTempFileFromUriStream(intent.getData());

        return path;

    }

    private String queryPathInUri(Uri uri) {

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

    private String createTempFileFromUriStream(Uri uri) {
        String path = null;
        InputStream is;

        try {

            // found this gem on the internet, this should handle Picassa image selections from the gallery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                is = new FileInputStream(fileDescriptor);
            } else {
                is = context.getContentResolver().openInputStream(uri);
            }

            File tempFile = createTemporaryFile("dat");
            FileOutputStream fos = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
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

    public File createTemporaryFile(String extension) throws IOException {
        File tempDir = context.getCacheDir();

        long ts = System.currentTimeMillis();
        return File.createTempFile("sudoku-" + ts, "." + extension.toLowerCase(), tempDir);
    }
}
