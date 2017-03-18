package com.danbuntu.sudokuisfun.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.danbuntu.sudokuisfun.R;

/**
 * Created by dan on 7/17/16. Have a great day!
 */
public class KeyboardFragment extends Fragment implements View.OnClickListener {

    Context mContext;
    int layoutRes;
    Button b1, b2, b3, b4, b5, b6, b7, b8, b9;
    ImageButton btnClear, btnToggle, btnPreview;
    View keyboardLayout;
    KeyPressListener keyListener;
    int mMode;
    int REQUESTED_MODE = -1;
    int possibilityColor;
    boolean mEditMode = false;
    boolean mSetValue = true;
    boolean mShowPreview = false;
    Bundle saveState = null;

    public static KeyboardFragment newInstance(int layoutRes) {
        Bundle bundle = new Bundle();
        bundle.putInt("LAYOUT", layoutRes);
        KeyboardFragment fragment = new KeyboardFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("LAYOUT")) {
            layoutRes = arguments.getInt("LAYOUT");
        } else {
            layoutRes = R.layout.layout_keyboard_small;
        }
        mMode = SudokuGridView.MODE_NUMBERS;

        if (savedInstanceState != null) saveState = savedInstanceState;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (saveState != null) {
            restoreSaveState(saveState);
            saveState = null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        keyListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        keyboardLayout = inflater.inflate(layoutRes, container, false);

        possibilityColor = getActivity().getResources().getColor(R.color.grid_text_possibility);

        b1 = (Button) keyboardLayout.findViewById(R.id.btn1);
        b2 = (Button) keyboardLayout.findViewById(R.id.btn2);
        b3 = (Button) keyboardLayout.findViewById(R.id.btn3);
        b4 = (Button) keyboardLayout.findViewById(R.id.btn4);
        b5 = (Button) keyboardLayout.findViewById(R.id.btn5);
        b6 = (Button) keyboardLayout.findViewById(R.id.btn6);
        b7 = (Button) keyboardLayout.findViewById(R.id.btn7);
        b8 = (Button) keyboardLayout.findViewById(R.id.btn8);
        b9 = (Button) keyboardLayout.findViewById(R.id.btn9);

        btnClear = (ImageButton) keyboardLayout.findViewById(R.id.btnClear);
        btnToggle = (ImageButton) keyboardLayout.findViewById(R.id.btnModeToggle);
        btnPreview = (ImageButton) keyboardLayout.findViewById(R.id.btnShowImage);
        btnPreview.setEnabled(false);

        b1.setOnClickListener(this);
        b2.setOnClickListener(this);
        b3.setOnClickListener(this);
        b4.setOnClickListener(this);
        b5.setOnClickListener(this);
        b6.setOnClickListener(this);
        b7.setOnClickListener(this);
        b8.setOnClickListener(this);
        b9.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnToggle.setOnClickListener(this);

        btnPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (keyListener == null) return false;
                switch (motionEvent.getActionMasked()) {
                    case (MotionEvent.ACTION_DOWN):
                        Log.i("FRAGMENT", "preview down");
                        keyListener.previewPressed();
                        break;
                    case (MotionEvent.ACTION_UP):
                        Log.i("FRAGMENT", "preview up");
                        keyListener.previewUnpressed();
                        break;
                }
                return true;
            }
        });

        return keyboardLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("SET_VALUE", mSetValue);
        outState.putInt("KEYBOARD_MODE", mMode);
        outState.putBoolean("EDIT_MODE", mEditMode);
        outState.putBoolean("SHOW_PREVIEW", mShowPreview);
        outState.putInt("REQUESTED_MODE", REQUESTED_MODE);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // cache the saveState so we can process in onStart()
        if (savedInstanceState != null) saveState = savedInstanceState;
    }

    private void restoreSaveState(Bundle bundle) {
        if (bundle != null) {
            // this order is crucial to restore the state correctly
            setValues(bundle.getBoolean("SET_VALUE", true));
            setDisplayMode(bundle.getInt("KEYBOARD_MODE", SudokuGridView.MODE_NUMBERS));
            setEditMode(bundle.getBoolean("EDIT_MODE", false));
            setImageLoaded(bundle.getBoolean("SHOW_PREVIEW", false));
            REQUESTED_MODE = bundle.getInt("REQUESTED_MODE", -1);
        }
    }

    @Override
    public void onClick(View view) {
        if (keyListener == null) return;

        int value = -1;

        switch (view.getId()) {
            case (R.id.btnClear):
                keyListener.clearPressed();
                return;
            case (R.id.btnModeToggle):
                setValues(!mSetValue);
                return;
            case (R.id.btn1):
                value = 1;
                break;
            case (R.id.btn2):
                value = 2;
                break;
            case (R.id.btn3):
                value = 3;
                break;
            case (R.id.btn4):
                value = 4;
                break;
            case (R.id.btn5):
                value = 5;
                break;
            case (R.id.btn6):
                value = 6;
                break;
            case (R.id.btn7):
                value = 7;
                break;
            case (R.id.btn8):
                value = 8;
                break;
            case (R.id.btn9):
                value = 9;
                break;
        }
        keyListener.numberPressed(value, mSetValue);
    }

    /**
     * Set whether or not the PuzzleActivity has a valid image that can be previewed with btnPreview
     *
     * @param set whether or not there is a valid image to be displayed
     *            if true the preview button will display and the toggle button will be hidden
     */
    public void setImageLoaded(boolean set) {
        mShowPreview = set;
        if (mEditMode) {
            btnPreview.setEnabled(set);
        } else {
            btnPreview.setEnabled(false);
        }
    }

    public int getLayoutId() {
        return layoutRes;
    }

    public void setKeyPressListener(KeyPressListener listener) {
        this.keyListener = listener;
    }

    /**
     * Convenience method to adjust attributes on a button
     *
     * @param button     The button object to modify
     * @param background Background drawable id to set on the button
     * @param textColor  Text color to set on the button
     * @param text       Text to set on the button
     */
    private void setKey(Button button, int background, int textColor, String text) {
        if (background != -1) button.setBackgroundResource(background);
        if (text != null) button.setText(text);
        if (textColor != -1) button.setTextColor(textColor);
    }

    /**
     * Display the keys needed for the SudokuGridView edit mode
     *
     * @param set True for edit mode, False for normal
     */
    public void setEditMode(boolean set) {
        if (set) {
            if (!mSetValue) setValues(true);
            if (mMode != SudokuGridView.MODE_NUMBERS) {
                // suspend current mode
                REQUESTED_MODE = mMode;
                setDisplayMode(SudokuGridView.MODE_NUMBERS);
            }
            btnToggle.setVisibility(View.INVISIBLE);
            btnPreview.setVisibility(View.VISIBLE);

            // important to set this AFTER we are finished in this method
            mEditMode = true;
            //

        } else {

            // important to set this BEFORE we continue this method
            mEditMode = false;
            //

            if (REQUESTED_MODE != -1) {
                // revert to the requested mode
                setDisplayMode(REQUESTED_MODE);
                REQUESTED_MODE = -1;
            }
            btnToggle.setVisibility(View.VISIBLE);
            btnPreview.setEnabled(false);
            btnPreview.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Set the keyboard to place cell possibilities, or values
     *
     * @param set True to set values, False to set possibilities
     */
    private void setValues(boolean set) {
        mSetValue = set;

        int drawable = (set) ? R.drawable.key_background : R.drawable.key_c1;

        if(mMode != SudokuGridView.MODE_COLORS) {
            setKey(b1, drawable, -1, null);
            setKey(b2, drawable, -1, null);
            setKey(b3, drawable, -1, null);
            setKey(b4, drawable, -1, null);
            setKey(b5, drawable, -1, null);
            setKey(b6, drawable, -1, null);
            setKey(b7, drawable, -1, null);
            setKey(b8, drawable, -1, null);
            setKey(b9, drawable, -1, null);
        }

        btnPreview.setBackgroundResource(drawable);
        btnToggle.setBackgroundResource(drawable);
        btnClear.setBackgroundResource(drawable);
    }

    /**
     * Set the keys to display the correct characters / colors that match the display mode the
     * SudokuGridView is currently in
     *
     * @param mode An integer display mode from SudokuGridView that corresponds to the grid's
     *             current display mode
     */
    public void setDisplayMode(int mode) {
        // don't touch anything if were in edit mode
        if (mEditMode) {
            REQUESTED_MODE = mode;
            return;
        }

        this.mMode = mode;
        int drawable = (mSetValue) ? R.drawable.key_background : R.drawable.key_c1;

        switch (mode) {
            case (SudokuGridView.MODE_COLORS):
                setKey(b1, R.drawable.key_c1, -1, "");
                setKey(b2, R.drawable.key_c2, -1, "");
                setKey(b3, R.drawable.key_c3, -1, "");
                setKey(b4, R.drawable.key_c4, -1, "");
                setKey(b5, R.drawable.key_c5, -1, "");
                setKey(b6, R.drawable.key_c6, -1, "");
                setKey(b7, R.drawable.key_c7, -1, "");
                setKey(b8, R.drawable.key_c8, -1, "");
                setKey(b9, R.drawable.key_c9, -1, "");
                break;

            case (SudokuGridView.MODE_LETTERS):
                setKey(b1, drawable, -1, "A");
                setKey(b2, drawable, -1, "B");
                setKey(b3, drawable, -1, "C");
                setKey(b4, drawable, -1, "D");
                setKey(b5, drawable, -1, "E");
                setKey(b6, drawable, -1, "F");
                setKey(b7, drawable, -1, "G");
                setKey(b8, drawable, -1, "H");
                setKey(b9, drawable, -1, "I");
                break;

            case (SudokuGridView.MODE_NUMBERS):
                setKey(b1, drawable, -1, "1");
                setKey(b2, drawable, -1, "2");
                setKey(b3, drawable, -1, "3");
                setKey(b4, drawable, -1, "4");
                setKey(b5, drawable, -1, "5");
                setKey(b6, drawable, -1, "6");
                setKey(b7, drawable, -1, "7");
                setKey(b8, drawable, -1, "8");
                setKey(b9, drawable, -1, "9");
                break;
        }
    }

    public interface KeyPressListener {
        void numberPressed(int value, boolean setValue);

        void clearPressed();

        void previewPressed();

        void previewUnpressed();
    }
}