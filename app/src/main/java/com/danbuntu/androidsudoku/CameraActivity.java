package com.danbuntu.androidsudoku;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Gives a simple UI that detects if this device has a camera,
 * informing the user if they do or dont
 * <p/>
 * This also receives the result of a picture being taken and displays it to the user
 *
 * @author paul.blundell
 */
public class CameraActivity extends AppCompatActivity
        implements Camera.PictureCallback {

    private Camera camera;
    private CameraView cameraView;
    private Overlay overlay;

    //TODO all of this is shit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ImageButton btnGallery = (ImageButton) findViewById(R.id.btnChooseFromGallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseCamera();
                dispatchGalleryActivity();
            }
        });

        ImageButton btnTakePicture = (ImageButton) findViewById(R.id.btnTakePicture);

        cameraView = (CameraView) findViewById(R.id.cameraView);

        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);


        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_LONG).show();
        }

        if (camera == null) {

            btnTakePicture.setEnabled(false);
            btnTakePicture.setClickable(false);
            Toast.makeText(this, "Couldn't access the camera", Toast.LENGTH_LONG).show();

        } else {

            btnTakePicture.setEnabled(true);
            btnTakePicture.setClickable(true);
            initCameraView(camera);

        }

        overlay = (Overlay) findViewById(R.id.overlay);

    }

    public void dispatchGalleryActivity() {

//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/*");
//        startActivityForResult(intent, IMAGE_FROM_GALLERY);

        Intent intent = new Intent(this, GalleryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);

        releaseCamera();
        finish();
    }

    private void initCameraView(Camera cam) {
        cameraView.init(cam);
    }

    public void onCaptureClick(View button) {
        if (camera != null) takePicture();
    }

    private void takePicture() {
        camera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d("CAMERAACTIVITY", "Picture taken");

        // save only the grid picture to storage
        String path = savePicture(data);

        returnPath(path);
    }

    private File getSaveFile() throws IOException {
        File out = new File(getExternalCacheDir(), "sudoku-capture.jpg");
        out.createNewFile();
        return out;
    }

    private String savePicture(byte[] data) {
        File out;
        FileOutputStream fos;

        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, null);
        // fix rotation from camera
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);

        Rect visibleArea = overlay.getVisibleArea();

        float visibleYScale = (float)visibleArea.height() / cameraView.getHeight();
        float visibleXScale = (float)visibleArea.width() / cameraView.getWidth();
        int visibleYPos = visibleArea.top - cameraView.getTop();
        int visibleXPos = visibleArea.left - cameraView.getLeft();


//        Log.i("CAMERA CROP", "wScaleFactor: " + wScaleFactor);
//        Log.i("CAMERA CROP", "hScaleFactor: " + hScaleFactor);
//
//        Log.i("CAMERA CROP", "visible.top: " + visibleArea.top);
//        Log.i("CAMERA CROP", "visible.left: " + visibleArea.left);
//
//        Log.i("CAMERA CROP", "visible.height: " + visibleArea.height());
//        Log.i("CAMERA CROP", "visible.width: " + visibleArea.width());
//
//        Log.i("CAMERA CROP", "bitmap.height: " + bmp.getHeight());
//        Log.i("CAMERA CROP", "bitmap.width: " + bmp.getWidth());
//
//        Log.i("CAMERA CROP", "new.top: " + bmpCropTop);
//        Log.i("CAMERA CROP", "new.left: " + bmpCropLeft);
//
//        Log.i("CAMERA CROP", "overlay.left: " + overlay.getLeft());
//        Log.i("CAMERA CROP", "overlay.top: " + overlay.getTop());
//        Log.i("CAMERA CROP", "overlay.right: " + overlay.getRight());
//        Log.i("CAMERA CROP", "overlay.bottom: " + overlay.getBottom());
//
//        Log.i("CAMERA CROP", "cameraView.left: " + cameraView.getLeft());
//        Log.i("CAMERA CROP", "cameraView.top: " + cameraView.getTop());
//        Log.i("CAMERA CROP", "cameraView.right: " + cameraView.getRight());
//        Log.i("CAMERA CROP", "cameraView.bottom: " + cameraView.getBottom());



        // crop only the grid
        bmp = Bitmap.createBitmap(bmp,
                visibleXPos,
                visibleYPos,
                (int)(bmp.getWidth() * visibleXScale),
                (int)(bmp.getHeight() * visibleYScale));


        try {
            out = getSaveFile();
            fos = new FileOutputStream(out);

            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.flush();
            fos.close();
            return out.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to save image", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private void returnPath(String path) {
//        Intent intent = new Intent(this, OCRActivity.class);
//        intent.putExtra(EXTRA_IMAGE_PATH, path);
//        intent.putExtra(EXTRA_MODE, OCRActivity.RETURN);
//        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
//        startActivity(intent);
//
//        releaseCamera();

        Intent intent = new Intent();
        intent.putExtra(getString(R.string.intent_extra_image_path), path);
        setResult(RESULT_OK, intent);

        finish();
    }

    // ALWAYS remember to release the camera when you are finished
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
