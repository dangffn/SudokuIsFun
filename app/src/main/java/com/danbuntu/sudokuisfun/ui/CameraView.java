package com.danbuntu.sudokuisfun.ui;

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
    final static int DISPLAY_ORIENTATION = 90;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void init(Camera cam) {
        camera = cam;
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private Camera.Size getOptimalSize(int horizontal, List<Camera.Size> supportedPreviewSizes) {

        int minWidthDifference = Integer.MAX_VALUE;
        Camera.Size optimalSize = null;

        if (supportedPreviewSizes != null) {
            for (Camera.Size size : supportedPreviewSizes) {
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

    private void initCamera(SurfaceHolder holder) {

        try {

            Camera.Size optimalPreviewSize;
            Camera.Size optimalPictureSize;

            camera.setPreviewDisplay(holder);
            camera.startPreview();

            Camera.Parameters params = camera.getParameters();

            optimalPictureSize = getOptimalSize(getWidth(), params.getSupportedPictureSizes());
            optimalPreviewSize = getOptimalSize(getWidth(), params.getSupportedPreviewSizes());

            params.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
            params.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);

            // enable continuous auto focus if available
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
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("CameraView", "SurfaceCreated");
        initCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.release();
    }
}
