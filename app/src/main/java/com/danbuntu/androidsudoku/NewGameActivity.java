package com.danbuntu.androidsudoku;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Dan on 6/7/2016.
 */
public class NewGameActivity extends AppCompatActivity {

    ListView listView;
    TemplateManager tm;
    Cursor cursor;
    SQLiteDatabase db;
    CustomCursorAdapter adapter;

    // if there will be a "New Game" and "Load Game" button in the main menu
    final static int LOAD_TEMPLATE = 0;
    final static int LOAD_SAVE = 1;

    final static int INTENT_GET_IMAGE_PATH = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        listView = (ListView) findViewById(R.id.lstNewGameSelectTemplate);

        TextView empty = (TextView) findViewById(R.id.txtEmpty);
        listView.setEmptyView(empty);

        //TODO add empty listener to listview to display an empty message

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String selectedName;
                if(cursor != null) {
                    cursor.moveToPosition(position);

                    selectedName = cursor.getString(cursor.getColumnIndex(TemplateManager.NAME_COL));

                    if(selectedName != null) {

                        // there is a save for this template, ask the user if they want to continue it
                        if(adapter.containsSave(selectedName)) {

                            AlertDialog.Builder ab = new AlertDialog.Builder(NewGameActivity.this);
                            ab.setTitle("Resume game?");
                            ab.setMessage("Do you want to resume your last game?");
                            ab.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // resume the last game
                                    startPuzzleActivity(selectedName, tm.getSave(selectedName));
                                    dialog.dismiss();

                                }
                            }).setNegativeButton("No, delete it", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // delete the last save and start a new game
                                    startPuzzleActivity(selectedName, tm.getTemplate(selectedName));
                                    dialog.dismiss();

                                }
                            }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // bail out
                                    dialog.cancel();

                                }
                            }).create().show();

                        } else {

                            // no save exists for this template
                            startPuzzleActivity(selectedName, tm.getTemplate(selectedName));

                        }

                    } else {
                        Log.e("NewGameActivity", "Got a null template name from TemplateManager");
                    }

                } else {
                    Log.e("NewGameActivity", "Cursor is null, cant get template");
                }
            }
        });

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            MenuItem rename;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_new_game_cab, menu);
                rename = menu.findItem(R.id.menu_cab_rename);
                adapter.setMultiSelect(true);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

                switch(item.getItemId()) {
                    case(R.id.menu_cab_delete):
                        String message = (adapter.getSelected().size() > 1) ? "Are you sure you want to delete these " + adapter.getSelected().size() + " puzzles?" :
                                "Are you sure you want to delete this puzzle?";

                        new AlertDialog.Builder(NewGameActivity.this).setTitle("Are you sure?").setMessage(message).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String toDelete;
                                for(int i : adapter.getSelected()) {
                                    cursor.moveToPosition(i);
                                    toDelete = cursor.getString(cursor.getColumnIndex(TemplateManager.NAME_COL));
                                    db.delete(TemplateManager.TEMPLATES_TABLE, TemplateManager.NAME_COL + "=?", new String[]{ toDelete });
                                }
                                cursor = db.query(TemplateManager.TEMPLATES_TABLE, null, null, null, null, null, null);
                                adapter.changeCursor(cursor);
                                adapter.resetSelected();
                                adapter.notifyDataSetChanged();

                                mode.finish();
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();

                        break;

                    case(R.id.menu_cab_cancel):
                        mode.finish();
                        break;

                    case(R.id.menu_cab_rename):

                        cursor.moveToPosition(adapter.getSelected().get(0));
                        final String name = cursor.getString(cursor.getColumnIndex(TemplateManager.NAME_COL));

                        AlertDialog.Builder ab = new AlertDialog.Builder(NewGameActivity.this);
<<<<<<< HEAD
                        LinearLayout ll = (LinearLayout) LayoutInflater.from(NewGameActivity.this).inflate(R.layout.dialog_text_box, null);
                        ab.setView(ll);
                        final EditText et = (EditText) ll.findViewById(R.id.dialog_text_box);
=======
                        LinearLayout ll = (LinearLayout) LayoutInflater.from(NewGameActivity.this).inflate(R.layout.dialog_rename, null);
                        ab.setView(ll);
                        final EditText et = (EditText) ll.findViewById(R.id.txtRename);
>>>>>>> 2c63def... Fixed alot of stuff
                        et.setHint(name);
                        et.setText(name);

                        ab.setTitle("Rename Puzzle").setMessage("Enter the new name for this puzzle").setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newName = et.getText().toString().trim();
                                if(newName.equals(name) || newName.equals("")) return;

                                ContentValues vals = new ContentValues();
                                vals.put(TemplateManager.NAME_COL, newName);
                                db.update(TemplateManager.TEMPLATES_TABLE, vals, TemplateManager.NAME_COL + "=?", new String[]{ name });

                                cursor = db.query(TemplateManager.TEMPLATES_TABLE, null, null, null, null, null, null);
                                adapter.changeCursor(cursor);
                                adapter.notifyDataSetChanged();
                                dialog.dismiss();
                                mode.finish();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        //TODO fix textview margins



                        ab.create().show();

                        break;

                    default:
                        return false;
                }

                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adapter.setMultiSelect(false);
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                adapter.toggleSelected(position);
                int count = adapter.selectedCount();
                mode.setTitle(count + " Selected");
                if(count > 1) {
                    rename.setEnabled(false);
                } else if(count == 0) {
                    mode.finish();
                } else {
                    rename.setEnabled(true);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_game, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case (R.id.menu_camera):
                Intent cameraIntent = new Intent(this, CameraActivity.class);
                startActivityForResult(cameraIntent, INTENT_GET_IMAGE_PATH);
                return true;
            case (R.id.menu_gallery):
                Intent galleryIntent = new Intent(this, GalleryActivity.class);
                startActivityForResult(galleryIntent, INTENT_GET_IMAGE_PATH);
                return true;
            case(R.id.menu_blank_puzzle):
                startPuzzleActivity();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == INTENT_GET_IMAGE_PATH) {

            final String path = data.getExtras().getString(getString(R.string.intent_extra_image_path));
            if (path != null) {

                Intent puzzleIntent = new Intent(this, PuzzleActivity.class);
                puzzleIntent.putExtra(PuzzleActivity.INTENT_IMAGE_PATH, path);
                puzzleIntent.putExtra(PuzzleActivity.INTENT_TEMPLATE_NAME, "From Picture");
                startActivity(puzzleIntent);

            }
        }
    }

    @Override
    protected void onResume() {

        tm = new TemplateManager(this);
        db = tm.getReadableDatabase();

        Log.i("NewGameActivity", "Loading database and cursor");
        cursor = db.query(TemplateManager.TEMPLATES_TABLE, null, null, null, null, null, null);
        adapter = new CustomCursorAdapter(this, R.layout.list_item_puzzle_save, cursor, -1);
        listView.setAdapter(adapter);

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i("NewGameActivity", "Closing database and cursor");
        cursor.close();
        db.close();
    }

    private void startPuzzleActivity() {
        Intent intent = new Intent(this, PuzzleActivity.class);
        startActivity(intent);
        finish();
    }

    private void startPuzzleActivity(String name, int[] puzzle) {
        if(puzzle == null) return;

        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_TEMPLATE);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_DATA, puzzle);
        intent.putExtra(PuzzleActivity.INTENT_TEMPLATE_NAME, name);
        startActivity(intent);
        finish();

    }

    private void startPuzzleActivity(String name, String puzzleJson) {
        if(puzzleJson == null) return;

        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_SAVE_STATE);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_DATA, puzzleJson);
        intent.putExtra(PuzzleActivity.INTENT_TEMPLATE_NAME, name);
        startActivity(intent);
        finish();

    }
}
