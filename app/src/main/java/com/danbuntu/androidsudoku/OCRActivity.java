package com.danbuntu.androidsudoku;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Dan on 4/28/2016.
 */

public class OCRActivity extends AppCompatActivity {

    OCRData ocrData;
    String imagePath;
    ImageView imageView, selectedRegion;
    Rect bitmapRect;
    Spinner spnDigit;
    CheckBox chkPreview;
    Bitmap gridDisplay;
    TextView txtStatus;
    int[] coords;
    boolean[][] learned;
    boolean selected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_ocractivity);
        imageView = (ImageView) findViewById(R.id.gridImageView);
        selectedRegion = (ImageView) findViewById(R.id.selectedImageView);

        txtStatus = (TextView) findViewById(R.id.txtPreviewStatus);

        spnDigit = (Spinner) findViewById(R.id.spnTrainNumber);
        coords = new int[6];

        learned = new boolean[GridSpecs.ROWS][GridSpecs.COLS];

        chkPreview = (CheckBox) findViewById(R.id.chkPreview);
        chkPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(ocrData != null && selectedRegion.getDrawable() != null) {
                    if (isChecked) {
                        selectedRegion.setImageBitmap(null);
                        OCR ocr = new OCR(ocrData.getRegionBitmap(coords[0], coords[1]));
                        ocr.prepare(true);
                        selectedRegion.setImageBitmap(ocr.getBitmap());
                    } else {
                        selectedRegion.setImageBitmap(ocrData.getRegionBitmap(coords[0], coords[1]));
                    }
                }
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case (MotionEvent.ACTION_DOWN):
//                        Log.i("ImageView", "ImageView received touchup at: " + event.getX() + " x " + event.getY());

                        if (bitmapRect != null && bitmapRect.contains((int) event.getX(), (int) event.getY())) {
//                            Log.i("ImageView", "ImageView received an on touch event inside the bitmap");

                            int sectionX = (int) ((event.getX() - bitmapRect.left) / ((float)bitmapRect.width() / GridSpecs.COLS));
                            int sectionY = (int) ((event.getY() - bitmapRect.top) / ((float)bitmapRect.height() / GridSpecs.ROWS));

                            coords[0] = sectionX;
                            coords[1] = sectionY;

                            int boxX = (int) (sectionX * ((float) bitmapRect.width() / GridSpecs.COLS));
                            int boxY = (int) (sectionY * ((float) bitmapRect.height() / GridSpecs.ROWS));

                            coords[2] = boxX;
                            coords[3] = boxY;
                            coords[4] = boxX + (bitmapRect.width() / GridSpecs.COLS);
                            coords[5] = boxY + (bitmapRect.height() / GridSpecs.ROWS);

                            Bitmap region = ocrData.getRegionBitmap(sectionX, sectionY);

                            OCR ocr = new OCR(region);
                            ocr.prepare(true);

                            if(chkPreview.isChecked()) {
                                selectedRegion.setImageBitmap(ocr.getBitmap());
                            } else {
                                selectedRegion.setImageBitmap(region);
                            }

                            if(!ocr.isBlank()) {
                                ocrData.scan(ocr, true);
                                txtStatus.setText("Value: [" + ocr.getValue() + "]");
                            } else {
                                if(!learned[coords[0]][coords[1]]) {
                                    colorCell(Color.RED);
                                    learned[coords[0]][coords[1]] = true;
                                }
                                txtStatus.setText("This cell is empty");
                            }

                            selected = true;

                            return true;
                        } else {
                            showImageDialog();
                        }
                }

                return false;
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                imagePath = bundle.getString(getString(R.string.intent_extra_image_path));
                ocrData = new OCRData(OCRActivity.this, imagePath);
            }
        }

        if (ocrData == null || ocrData.gridBitmap == null) {

            showImageDialog();

        } else {
            displayImage();
        }
    }

    private void colorCell(int color) {
        Canvas canvas = new Canvas(gridDisplay);
        Paint paint = new Paint();
        paint.setColor(color & 0x88FFFFFF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(coords[2], coords[3], coords[4], coords[5], paint);
        imageView.setImageBitmap(gridDisplay);
    }

    public void trainOCR(View view) {
        if (ocrData != null && selected && !learned[coords[0]][coords[1]]) {

            ocrData.save(coords[0], coords[1], Integer.valueOf(spnDigit.getSelectedItem().toString()));

            // prevents same digit from being learned twice
            learned[coords[0]][coords[1]] = true;

            colorCell(Color.GREEN);

            Toast.makeText(this, spnDigit.getSelectedItem().toString() + " trained", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImage() {
        if (ocrData == null || ocrData.gridBitmap == null) return;
        imageView.setImageBitmap(ocrData.gridBitmap);
        bitmapRect = Overlay.getBitmapRect(imageView, ocrData.gridBitmap);
        gridDisplay = Bitmap.createScaledBitmap(ocrData.gridBitmap, bitmapRect.width(), bitmapRect.height(), false);
        imageView.setImageBitmap(gridDisplay);
        learned = new boolean[GridSpecs.ROWS][GridSpecs.COLS];
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
//        ab.setCancelable(false);
        ab.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == NewGameActivity.INTENT_GET_IMAGE_PATH) {

            String path = data.getExtras().getString(getString(R.string.intent_extra_image_path));
            if (path != null) {
                ocrData = new OCRData(this, path);
                if (ocrData.gridBitmap != null)
                    displayImage();

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
