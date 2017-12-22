package com.danbuntu.sudokuisfun.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.danbuntu.sudokuisfun.ocr.OCRScanner;
import com.danbuntu.sudokuisfun.puzzle.GridSpecs;

/**
 * Created by Dan on 6/15/2016. Have a great day!
 */
public class ProgressFragment extends DialogFragment {

    private Thread mOcrThread;
    Handler handler;
    private ProgressCallbacks mCallbacks;
    boolean started = false;
    private final static int TICK = 1;

    interface ProgressCallbacks {
        void onFragmentComplete(int[] array, Bitmap image);
    }

    public static ProgressFragment newInstance(String filePath) {

        Bundle args = new Bundle();
        args.putString("PATH", filePath);

        ProgressFragment frag = new ProgressFragment();

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("DialogFragment", "OnCreate triggered");
        setRetainInstance(true);
        Bundle args = getArguments();
        if(args == null) return;

        String filePath = args.getString("PATH");
        final OCRScanner ocrScanner = new OCRScanner(getActivity(), filePath);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                if(msg.what == TICK) {
                    ProgressDialog dialog = ((ProgressDialog)getDialog());
                    if(dialog != null) {
                        dialog.incrementProgressBy(1);
                        if(dialog.getProgress() == dialog.getMax()) {
                            dialog.dismiss();
                        }
                    }
                    return;
                }

                Bundle data = msg.getData();
                if (data != null && mCallbacks != null) {
                    mCallbacks.onFragmentComplete(data.getIntArray("GRID"), (Bitmap) data.getParcelable("IMAGE"));
                }
            }
        };

        mOcrThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ocrScanner.setScanListener(new OCRScanner.ScanListener() {
                    @Override
                    public void onCellFinished() {
                        handler.sendEmptyMessage(TICK);
                    }
                });
                ocrScanner.beginScan();
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putIntArray("GRID", ocrScanner.getGridData());
                bundle.putParcelable("IMAGE", ocrScanner.getGridBitmap());
                message.setData(bundle);
                handler.sendMessage(message);
            }
        });

    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (ProgressCallbacks) activity;
    }

    @Override
    public void onDetach() {
        Log.i("FRAGMENT", "onDetach called");
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle("Recognizing");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(GridSpecs.ROWS * GridSpecs.COLS);
        dialog.setCancelable(false);
        return dialog;

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("DialogFragment", "OnCreateView triggered");
        if(!started) {
            Log.i("DialogFragment", "Thread started");
            mOcrThread.start();
            started = true;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
