package com.danbuntu.sudokuisfun.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Dan on 5/12/2016.
 */
public class GalleryActivity extends AppCompatActivity {

    final static int IMAGE_FROM_GALLERY = 2;
    ImageView imageView;
    Overlay overlay;
    Bitmap bitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        imageView = (ImageView) findViewById(R.id.imageView);
        overlay = (Overlay) findViewById(R.id.resizableOverlay);
        if (overlay != null)
            overlay.setResizable();

        ImageButton btnCrop = (ImageButton) findViewById(R.id.btnCropPicture);
        if (btnCrop != null) {
            btnCrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String path = saveTrimmedBitmap();
                    if (path != null)
                        returnPath(path);
                }
            });
        }

        ImageButton btnCamera = (ImageButton) findViewById(R.id.btnChooseFromCamera);
        assert btnCamera != null;
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchCameraActivity();
                finish();
            }
        });

        if (SudokuUtils.checkReadPermissions(this)) {
            getImageFromGallery();
        } else {
            SudokuUtils.requestReadPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == SudokuUtils.READ_STORAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // external read permission was asked for and APPROVED by the user
                Log.i("GalleryActivity", "Permission to the external storage was granted by the user");
                getImageFromGallery();
            } else {
                // external read permission was asked for and DENIED by the user
                Toast.makeText(this, "Unable to access pictures", Toast.LENGTH_LONG).show();
                Log.e("GalleryActivity", "Permission to the external storage was denied by the user");
                finish();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void dispatchCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void returnPath(String path) {
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.intent_extra_imagePath), path);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void getImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_FROM_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_FROM_GALLERY) {
                imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        int maxSize = Math.max(imageView.getWidth(), imageView.getHeight());

                        Log.i("GalleryActivity", "Max image size is: " + maxSize);
                        bitmap = SudokuUtils.resolveBitmap(data, GalleryActivity.this, maxSize);

                        imageView.setImageBitmap(bitmap);

                        Rect rect = Overlay.getBitmapRect(imageView, bitmap);
                        overlay.setSelectionBounds(rect);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });

            } else {
                // if we didnt pick something from the gallery close the activity
                // theres no reason to stay open
                finish();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private File getSaveFile() throws IOException {
        File file = new File(getCacheDir(), "image-cropped.png");

        if (file.createNewFile())
            Log.i("GalleryActivity", "Created cached crop image");

        return file;
    }

    private String saveTrimmedBitmap() {

        if (bitmap == null) return null;

        Rect selection = overlay.getVisibleArea();
        Rect selectionBounds = overlay.getSelectionBounds();

        float bitmapStartYScale = (float) (selection.top - selectionBounds.top) / selectionBounds.height();
        float bitmapStartXScale = (float) (selection.left - selectionBounds.left) / selectionBounds.width();
        float bitmapWScale = (float) selection.width() / selectionBounds.width();
        float bitmapHScale = (float) selection.height() / selectionBounds.height();

        bitmap = Bitmap.createBitmap(bitmap, (int) (bitmapStartXScale * bitmap.getWidth()), (int) (bitmapStartYScale * bitmap.getHeight()),
                (int) (bitmapWScale * bitmap.getWidth()), (int) (bitmapHScale * bitmap.getHeight()));

        File outFile;
        try {

            FileOutputStream fos = new FileOutputStream(outFile = getSaveFile());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            bitmap.recycle();
            bitmap = null;

            return outFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();

        }

        return null;
    }
}
