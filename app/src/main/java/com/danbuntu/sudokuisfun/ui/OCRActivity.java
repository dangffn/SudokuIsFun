package com.danbuntu.sudokuisfun.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.ocr.OCRScanner;
import com.danbuntu.sudokuisfun.ocr.OCRUnit;
import com.danbuntu.sudokuisfun.puzzle.GridSpecs;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

/**
 * Created by Dan on 4/28/2016. Have a great day!
 */

public class OCRActivity extends AppCompatActivity {

    OCRScanner ocrScanner;
    ImageView imageView, selectedRegion;
    Rect bitmapRect;
    Spinner spnDigit;
    CheckBox chkPreview;
    Bitmap gridDisplay;
    TextView txtStatus;
    int[] coord;
    boolean[][] learnedCells;
    boolean selected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_training);
        imageView = (ImageView) findViewById(R.id.gridImageView);
        selectedRegion = (ImageView) findViewById(R.id.selectedImageView);

        txtStatus = (TextView) findViewById(R.id.txtPreviewStatus);

        spnDigit = (Spinner) findViewById(R.id.spnTrainNumber);
        coord = new int[6];

        learnedCells = new boolean[GridSpecs.ROWS][GridSpecs.COLS];

        chkPreview = (CheckBox) findViewById(R.id.chkPreview);
        chkPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ocrScanner != null && selectedRegion.getDrawable() != null) {
                    if (isChecked) {
                        selectedRegion.setImageBitmap(null);
                        OCRUnit ocr = new OCRUnit(ocrScanner.getRegionBitmap(coord[0], coord[1]));
                        ocr.prepare(true);
                        selectedRegion.setImageBitmap(ocr.getBitmap());
                    } else {
                        selectedRegion.setImageBitmap(ocrScanner.getRegionBitmap(coord[0], coord[1]));
                    }
                }
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case (MotionEvent.ACTION_DOWN):
                        if (bitmapRect != null && bitmapRect.contains((int) event.getX(), (int) event.getY())) {
                            int sectionX = (int) ((event.getX() - bitmapRect.left) / ((float) bitmapRect.width() / GridSpecs.COLS));
                            int sectionY = (int) ((event.getY() - bitmapRect.top) / ((float) bitmapRect.height() / GridSpecs.ROWS));

                            coord[0] = sectionX;
                            coord[1] = sectionY;

                            int boxX = (int) (sectionX * ((float) bitmapRect.width() / GridSpecs.COLS));
                            int boxY = (int) (sectionY * ((float) bitmapRect.height() / GridSpecs.ROWS));

                            coord[2] = boxX;
                            coord[3] = boxY;
                            coord[4] = boxX + (bitmapRect.width() / GridSpecs.COLS);
                            coord[5] = boxY + (bitmapRect.height() / GridSpecs.ROWS);

                            Bitmap region = ocrScanner.getRegionBitmap(sectionX, sectionY);

                            OCRUnit ocr = new OCRUnit(region);
                            ocr.prepare(true);

                            if (chkPreview.isChecked()) {
                                selectedRegion.setImageBitmap(ocr.getBitmap());
                            } else {
                                selectedRegion.setImageBitmap(region);
                            }

                            if (!ocr.isBlank()) {
                                ocrScanner.scan(ocr, true);
                                txtStatus.setText(String.format(getString(R.string.thisLooksLikeA), ocr.getValue()));
                                spnDigit.setSelection(ocr.getValue() - 1);
                            } else {
                                if (!learnedCells[coord[0]][coord[1]]) {
                                    colorCell(Color.RED);
                                    learnedCells[coord[0]][coord[1]] = true;
                                }
                                txtStatus.setText(getString(R.string.cellIsEmpty));
                            }

                            selected = true;

                            return true;
                        }
                }

                return true;
            }
        });

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_ocr_first_run), false)) {
            showHowToDialog();
        } else {
            showImageDialog();
        }

        if (!SudokuUtils.checkWritePermissions(this)) SudokuUtils.requestWritePermission(this);
    }

    private void colorCell(int color) {
        Canvas canvas = new Canvas(gridDisplay);
        Paint paint = new Paint();
        paint.setColor(color & 0x88FFFFFF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(coord[2], coord[3], coord[4], coord[5], paint);
        imageView.setImageBitmap(gridDisplay);
    }

    public void trainOCR(View view) {
        if (ocrScanner != null && selected && !learnedCells[coord[0]][coord[1]]) {

            // process the data and save to the learning file
            ocrScanner.saveLearnedOCRData(coord[0], coord[1], Integer.valueOf(spnDigit.getSelectedItem().toString()));

            // increment the signature tally in the preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int sigCount = prefs.getInt(getString(R.string.pref_key_externalSigCount), 0);
            SharedPreferences.Editor prefsEdit = prefs.edit();
            prefsEdit.putInt(getString(R.string.pref_key_externalSigCount), sigCount + 1);
            prefsEdit.apply();

            // prevents same digit from being learnedCells twice
            learnedCells[coord[0]][coord[1]] = true;

            colorCell(Color.GREEN);

            Toast.makeText(this, spnDigit.getSelectedItem().toString() + " trained", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImage() {
        if (ocrScanner == null || ocrScanner.getGridBitmap() == null) return;
        imageView.setImageBitmap(ocrScanner.getGridBitmap());
        bitmapRect = Overlay.getBitmapRect(imageView, ocrScanner.getGridBitmap());
        if (bitmapRect != null)
            gridDisplay = Bitmap.createScaledBitmap(ocrScanner.getGridBitmap(), bitmapRect.width(), bitmapRect.height(), false);
        imageView.setImageBitmap(gridDisplay);
        learnedCells = new boolean[GridSpecs.ROWS][GridSpecs.COLS];
        selectedRegion.setImageBitmap(null);
    }

    private void showImageDialog() {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setItems(new String[]{"Select from gallery", "Take a picture"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case (0):
                        Intent galleryIntent = new Intent(OCRActivity.this, GalleryActivity.class);
                        startActivityForResult(galleryIntent, NewGameActivity.INTENT_GET_IMAGE_PATH);
                        break;
                    case (1):
                        Intent cameraIntent = new Intent(OCRActivity.this, CameraActivity.class);
                        startActivityForResult(cameraIntent, NewGameActivity.INTENT_GET_IMAGE_PATH);
                        break;
                }
            }
        });
        ab.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ocr_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.menu_camera):
                Intent cameraIntent = new Intent(OCRActivity.this, CameraActivity.class);
                startActivityForResult(cameraIntent, NewGameActivity.INTENT_GET_IMAGE_PATH);
                break;
            case (R.id.menu_gallery):
                Intent galleryIntent = new Intent(OCRActivity.this, GalleryActivity.class);
                startActivityForResult(galleryIntent, NewGameActivity.INTENT_GET_IMAGE_PATH);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SudokuUtils.WRITE_STORAGE_REQUEST) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Unable to write to storage, access denied", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == NewGameActivity.INTENT_GET_IMAGE_PATH) {

            String path = data.getExtras().getString(getString(R.string.intent_extra_imagePath));
            if (path != null) {
                ocrScanner = new OCRScanner(this, path);
                if (ocrScanner.getGridBitmap() != null) {
                    displayImage();
                }

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showHowToDialog() {
        new AlertDialog.Builder(this)
                .setTitle("OCR Trainer")
                .setMessage(getString(R.string.ocrActivityWelcomeMessage))
                .setPositiveButton(getString(R.string.button_got_it), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(OCRActivity.this).edit();
                        editor.putBoolean(getString(R.string.pref_ocr_first_run), true);
                        editor.apply();
                        dialogInterface.dismiss();
                    }
                })
                .create().show();
    }
}
