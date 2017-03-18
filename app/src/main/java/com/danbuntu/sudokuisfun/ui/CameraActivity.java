package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity
        implements Camera.PictureCallback {

    private Camera camera;
    private CameraView cameraView;
    private Overlay overlay;
    ImageButton btnTakePicture;
    OrientationListener orientationListener;
    ImageButton btnGallery;
    boolean hasPermission;
    int currentOrientation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        btnGallery = (ImageButton) findViewById(R.id.btnChooseFromGallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeCamera();
                dispatchGalleryActivityAndFinish();
            }
        });

        orientationListener = new OrientationListener(this);
        btnTakePicture = (ImageButton) findViewById(R.id.btnTakePicture);
        cameraView = (CameraView) findViewById(R.id.cameraView);
        overlay = (Overlay) findViewById(R.id.overlay);

        if(hasPermission = SudokuUtils.checkCameraPermissions(this)) {
            openCamera();
        } else {
            SudokuUtils.requestCameraPermissions(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        orientationListener.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(hasPermission) openCamera();
        orientationListener.enable();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        // save only the grid picture to storage
        String path = savePicture(data);

        returnPath(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == SudokuUtils.CAMERA_PERMISSION_REQUEST) {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // camera permission was asked for and APPROVED by the user
                    Log.i("CameraActivity", "Permission to the camera was granted by the user");
                    hasPermission = true;
                    openCamera();
                } else {
                    // camera permission was asked for and DENIED by the user
                    Toast.makeText(this, "Couldn't access the camera", Toast.LENGTH_LONG).show();
                    Log.e("CameraActivity", "Permission to the camera was denied by the user");
                    hasPermission = false;
                    finish();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean openCamera() {
        if (camera != null) closeCamera();

        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Can't connect to the camera", Toast.LENGTH_LONG).show();
        }

        if (camera != null) {

            btnTakePicture.setEnabled(true);
            btnTakePicture.setClickable(true);
            initCameraView(camera);
            cameraView.setVisibility(View.VISIBLE);
            return true;

        } else {

            btnTakePicture.setEnabled(false);
            btnTakePicture.setClickable(false);
            return false;

        }
    }

    public void dispatchGalleryActivityAndFinish() {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
        closeCamera();
        finish();
    }

    private void initCameraView(Camera cam) {
        if (camera != null) cameraView.init(cam);
    }

    public void onCaptureClick(View button) {
        if (camera != null) takePicture();
    }

    private void takePicture() {
        if (camera != null) camera.takePicture(null, null, this);
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
        matrix.setRotate(CameraView.DISPLAY_ORIENTATION);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, null);

        // fix rotation from camera
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);

        Rect visibleArea = overlay.getVisibleArea();

        float visibleYScale = (float) visibleArea.height() / cameraView.getHeight();
        float visibleXScale = (float) visibleArea.width() / cameraView.getWidth();
        int visibleYPos = visibleArea.top - cameraView.getTop();
        int visibleXPos = visibleArea.left - cameraView.getLeft();

        // crop only the grid and rotate the grid to the phones orientation
        switch(currentOrientation) {
            case(OrientationListener.ROTATION_90):
                matrix.setRotate(90);
                break;
            case(OrientationListener.ROTATION_270):
                matrix.setRotate(270);
                break;
            case(OrientationListener.ROTATION_0):
                matrix.setRotate(0);
                break;
            case(OrientationListener.ROTATION_180):
                matrix.setRotate(180);
                break;
            default:
                matrix.setRotate(currentOrientation);
        }
        bmp = Bitmap.createBitmap(bmp,
                visibleXPos,
                visibleYPos,
                (int) (bmp.getWidth() * visibleXScale),
                (int) (bmp.getHeight() * visibleYScale),
                matrix,
                false);

        try {
            out = getSaveFile();
            fos = new FileOutputStream(out);

            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            return out.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to save image", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private void returnPath(String path) {
        Intent intent = new Intent();
        intent.putExtra(getString(R.string.intent_extra_imagePath), path);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void closeCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public class OrientationListener extends OrientationEventListener {

        final static int ROTATION_0 = 0;
        final static int ROTATION_90 = 270;
        final static int ROTATION_180 = 180;
        final static int ROTATION_270 = 90;

        public OrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int newRotation;

            if (orientation < 22.5 || orientation > 315) {
                newRotation = ROTATION_0;
            } else if (orientation > 67.5 && orientation < 112.5) {
                newRotation = ROTATION_90;
            } else if (orientation > 157.5 && orientation < 202.5) {
                newRotation = ROTATION_180;
            } else if (orientation > 247.5 && orientation < 292.5) {
                newRotation = ROTATION_270;
            } else {
                return;
            }

            if (newRotation != currentOrientation) {

                // this lets the rotation go in a full circle without misbehaving
                if(newRotation == 270 && currentOrientation == 0) {
                    currentOrientation = 360;
                } else if(newRotation == 0 && currentOrientation == 270) {
                    currentOrientation = -90;
                }

                Animation rotate1 = new RotateAnimation(
                        currentOrientation,
                        newRotation,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);

                Animation rotate2 = new RotateAnimation(
                        currentOrientation,
                        newRotation,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);

                rotate1.setFillAfter(true);
                rotate1.setDuration(300);
                rotate2.setFillAfter(true);
                rotate2.setDuration(300);

                btnTakePicture.startAnimation(rotate1);
                btnGallery.startAnimation(rotate2);
                currentOrientation = newRotation;

            }
        }
    }
}
