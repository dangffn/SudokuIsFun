package com.danbuntu.sudokuisfun.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

/**
 * Created by Dan on 6/7/2016. Have a great day!
 */
public class NewGameActivity extends AppCompatActivity {

    final static int INTENT_GET_IMAGE_PATH = 1;
    final static int CREATE_NEW_PUZZLE = 2;
    ViewPager viewPager;
    SwipeAdapter adapter;
    PuzzleManager pm;
    boolean internetPermission;
    int onLoadTabIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        pm = PuzzleManager.getInstance(this);

        viewPager = (ViewPager) findViewById(R.id.viewPager);

        String[] tabs = getResources().getStringArray(R.array.categories);

        adapter = new SwipeAdapter(getSupportFragmentManager(), tabs);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // kill the action mode so it doesn't stay open with the fragment off screen
                Log.i("ViewPager", "Page Selected Triggered");
                adapter.killActionMode();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        //

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (onLoadTabIndex > -1 && (onLoadTabIndex < adapter.getCount())) {
            viewPager.setCurrentItem(onLoadTabIndex);
            onLoadTabIndex = -1;
        } else {
            onLoadTabIndex = -1;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("NewGameActivity", "Closing database");
        pm.close();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!(internetPermission = SudokuUtils.checkInternetPermissions(this))) {
            SudokuUtils.requestInternetPermissions(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_game, menu);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.i("NewGameActivity", "This device does not have a camera, disabling the option");
            MenuItem camera = menu.findItem(R.id.menu_camera);
            camera.setEnabled(false);
            camera.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.menu_camera):
                Intent cameraIntent = new Intent(this, CameraActivity.class);
                startActivityForResult(cameraIntent, INTENT_GET_IMAGE_PATH);
                return true;
            case (R.id.menu_gallery):
                Intent galleryIntent = new Intent(this, GalleryActivity.class);
                startActivityForResult(galleryIntent, INTENT_GET_IMAGE_PATH);
                return true;
            case (R.id.menu_blank_puzzle):
                startPuzzleActivity();
                break;
            case (R.id.menu_cab_obfuscate):
                showRandomPuzzleDialog();
                break;
            case(R.id.menu_settings):
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showRandomPuzzleDialog() {

        String[] categories = getResources().getStringArray(R.array.categories);
        String[] items = new String[categories.length + 1];
        System.arraycopy(categories, 0, items, 0, categories.length);
        items[items.length - 1] = getString(R.string.any_category);
        final String[] finalItems = items;

        new AlertDialog.Builder(this)
                .setTitle("Create a Random Puzzle")
//                .setMessage("Create a random puzzle for which difficulty?")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String selectedItem = finalItems[i];
                        int[] puzzleData = pm.getRandomPuzzle(selectedItem);
                        if (puzzleData == null) {
                            Toast.makeText(NewGameActivity.this, "Failed to load random puzzle", Toast.LENGTH_LONG).show();
                            dialogInterface.cancel();
                            return;
                        }

                        String newPuzzleName = pm.nextAvailableName("Randomized Puzzle");
                        boolean saveIcon = !PreferenceManager.getDefaultSharedPreferences(NewGameActivity.this).getBoolean(getString(R.string.pref_key_useDefaultPuzzleIcon), false);
                        int[] newPuzzleData =
                                SudokuUtils.createNewPuzzle(
                                        NewGameActivity.this,
                                        pm,
                                        newPuzzleName,
                                        puzzleData,
                                        getString(R.string.puzzle_category_myPuzzles),
                                        true,
                                        saveIcon);

                        startPuzzleActivity(newPuzzleName, newPuzzleData);

                        dialogInterface.dismiss();

                    }
                })
                .setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create().show();

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    private void startPuzzleActivity() {
        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.NEW_BLANK_PUZZLE);
        startActivityForResult(intent, CREATE_NEW_PUZZLE);
    }

    private void startPuzzleActivity(String name, int[] puzzle) {
        if (puzzle == null) return;
        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_TEMPLATE);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_DATA, puzzle);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_NAME, name);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();

            if (requestCode == INTENT_GET_IMAGE_PATH) {

                String path;
                if (extras != null && (path = extras.getString(getString(R.string.intent_extra_imagePath))) != null) {
                    Intent puzzleIntent = new Intent(this, PuzzleActivity.class);
                    puzzleIntent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_IMAGE);
                    puzzleIntent.putExtra(PuzzleActivity.INTENT_IMAGE_PATH, path);
                    puzzleIntent.putExtra(PuzzleActivity.INTENT_PUZZLE_NAME, "From Picture");
                    startActivityForResult(puzzleIntent, CREATE_NEW_PUZZLE);
                }

            } else if (requestCode == CREATE_NEW_PUZZLE) {

                String tab;
                if (extras != null && (tab = extras.getString(getString(R.string.intent_extra_tab))) != null) {
                    onLoadTabIndex = adapter.getItemPosition(tab);
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SudokuUtils.INTERNET_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // internet permission was asked for and APPROVED by the user
                Log.i("NewGameActivity", "Permission to the internet was granted by the user");
                internetPermission = true;
            } else {
                // internet permission was asked for and DENIED by the user
                Log.e("NewGameActivity", "Permission to the internet was denied by the user");
                internetPermission = false;
//                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}


