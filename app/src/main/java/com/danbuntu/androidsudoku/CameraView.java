package com.danbuntu.androidsudoku;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by Dan on 4/24/2016.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceHolder holder;
    Context context;
    int DISPLAY_ORIENTATION = 90;

    //TODO this is also shitty

    public CameraView(Context context) {
        super(context);
        this.context = context;
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void init(Camera camera) {

        this.camera = camera;

        initSurfaceHolder();
    }

    @SuppressWarnings("deprecation") // needed for < 3.0
    private void initSurfaceHolder() {
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void listOptimalSizes() {
        for (Camera.Size size : camera.getParameters().getSupportedPreviewSizes()) {
            Log.i("SUPPORTED PREVIEW SIZES", size.width + " x " + size.height + " aspect: " + ((float) size.width / size.height));
        }

        for (Camera.Size size : camera.getParameters().getSupportedPictureSizes()) {
            Log.i("SUPPORTED PICTURE SIZES", size.width + " x " + size.height + " aspect: " + ((float) size.width / size.height));
        }

        Camera.Size optimalPreview = getOptimalPreviewSize(getWidth(), getHeight());
        Camera.Size optimalPicture = getOptimalPictureSize(getWidth(), getHeight());

        Log.i("OPTIMAL PREVIEW SIZE: ", optimalPreview.width + " x " + optimalPreview.height + " aspect: " + ((float) optimalPreview.width / optimalPreview.height));
        Log.i("OPTIMAL PICTURE SIZE: ", optimalPicture.width + " x " + optimalPicture.height + " aspect: " + ((float) optimalPicture.width / optimalPicture.height));
        Log.i("HEIGHT x WIDTH", getWidth() + " x " + getHeight());

    }

    private Camera.Size getOptimalPreviewSize(int horizontal, int vertical) {

        int minWidthDifference = Integer.MAX_VALUE;
        Camera.Size optimalSize = null;

        List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();

        if (supportedPreviewSizes != null) {
            for (Camera.Size size : camera.getParameters().getSupportedPreviewSizes()) {
                if (Math.abs(size.height - horizontal) < minWidthDifference) {
                    optimalSize = size;
                    minWidthDifference = Math.abs(size.height - horizontal);
                }
            }
        }

        return optimalSize;

    }

    private Camera.Size getOptimalPictureSize(int horizontal, int vertical) {

        int minWidthDifference = Integer.MAX_VALUE;
        Camera.Size optimalSize = null;

        List<Camera.Size> supportedPictureSizes = camera.getParameters().getSupportedPictureSizes();

        if (supportedPictureSizes != null) {
            for (Camera.Size size : camera.getParameters().getSupportedPictureSizes()) {
                if (Math.abs(size.height - horizontal) < minWidthDifference) {
                    optimalSize = size;
                    minWidthDifference = Math.abs(size.height - horizontal);
                }
            }
        }

        return optimalSize;

    }

    private void resizeToCameraPreview(int width, int height) {
        ViewGroup.LayoutParams lp = getLayoutParams();

        // height and width are inverted on purpose (camera is rotated 90 degrees)
        lp.width = height;
        lp.height = width;
        setLayoutParams(lp);
        Log.i("RESIZE", "Resized cameraview to: " + width + " x " + height);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera(holder);
    }

    private void initCamera(SurfaceHolder holder) {
        Camera.Size optimalPreviewSize;
        Camera.Size optimalPictureSize;

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();

            Camera.Parameters params = camera.getParameters();

            optimalPictureSize = getOptimalPictureSize(getWidth(), getHeight());
            optimalPreviewSize = getOptimalPreviewSize(getWidth(), getHeight());
            params.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
            params.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);

            // enable auto focus if available
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                Log.i("CAMERA", "Camera supports continuous auto focus, GOOD!");
            } else {
                Log.i("CAMERA", "Camera does not support continuous auto focus, this may look ugly!");
            }

            // apply the parameters to the camera
            camera.setParameters(params);
            camera.setDisplayOrientation(DISPLAY_ORIENTATION);

            resizeToCameraPreview(optimalPreviewSize.width, optimalPreviewSize.height);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.release();
    }
}
