package com.danbuntu.androidsudoku;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
        setContentView(R.layout.layout_gallery_activity);

        imageView = (ImageView) findViewById(R.id.imageView);
        overlay = (Overlay) findViewById(R.id.resizableOverlay);
        assert overlay != null;
        overlay.setResizable();

        Button btnCrop = (Button) findViewById(R.id.btnCropPicture);
        assert btnCrop != null;
        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String path = saveTrimmedBitmap();
                if (path != null)
                    returnPath(path);

            }
        });

        getImageFromGallery();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void returnPath(String path) {
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.intent_extra_image_path), path);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void getImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_FROM_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == IMAGE_FROM_GALLERY) {

            GalleryGrab gg = new GalleryGrab(data, this);
            imageView.setImageBitmap(bitmap = gg.resolveBitmap());

            imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    Rect rect = Overlay.getBitmapRect(imageView, bitmap);
                    overlay.setSelectionBounds(rect);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });

        } else if(requestCode == IMAGE_FROM_GALLERY) {

            // if we didnt pick something from the gallery close the activity
            // theres no reason to stay open
            finish();

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
