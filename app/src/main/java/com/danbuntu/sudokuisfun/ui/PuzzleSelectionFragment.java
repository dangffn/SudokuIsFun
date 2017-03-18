package com.danbuntu.sudokuisfun.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

public class PuzzleSelectionFragment extends Fragment {

    Cursor mCursor;
    PuzzleManager pm;
    PuzzleItemAdapter mAdapter;
    String mCategory;
    Context mContext;
    ListView listView;
    ActionMode mActionMode;
    OnFragmentChangeListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        Log.d("Fragment", "OnAttach");
        super.onAttach(context);
        mContext = context;
        // leave this instance open, parent activity will handle closing it
        pm = PuzzleManager.getInstance(context);
        mCursor = getCursor();

        boolean useDefaultIcon = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_key_useDefaultPuzzleIcon),
                false
        );
        mAdapter = new PuzzleItemAdapter(mContext, R.layout.listitem_puzzle, mCursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, useDefaultIcon);
    }

    @Override
    public void onDetach() {
        Log.d("Fragment", "OnDetach");
        super.onDetach();
        mCursor.close();
    }

    private Cursor getCursor() {
        Cursor cursor = null;

        if (pm != null) {
            SQLiteDatabase db = pm.getReadableDatabase();

            if (mCategory != null) {
                cursor = db.query(PuzzleManager.PUZZLES_TABLE, null, PuzzleManager.PUZZLES_CATEGORY_COL + " = ?", new String[]{mCategory}, null, null, null);
            } else {
                cursor = db.query(PuzzleManager.PUZZLES_TABLE, null, null, null, null, null, null);
            }
        } else {
            Log.e("Fragment", "Failed to get cursor, PuzzleManager is null");
        }

        return cursor;
    }

    public void requeryCursor() {

        if (pm == null) {
            Log.e("Fragment", "Unable to re-query cursor, pm is null");
            return;
        }

        mCursor = getCursor();

        if (mAdapter != null && mCursor != null) {

            mAdapter.changeCursor(mCursor);
            mAdapter.notifyDataSetChanged();
            Log.i("Fragment", "Cursor successfully re-queried");

        } else if (mAdapter == null) {
            Log.e("Fragment", "Unable to re-query adapter, mAdapter is null");
        } else {
            Log.e("Fragment", "Unable to re-query adapter, mCursor is null");
        }
    }

    public void setCategoryFilter(String categoryString) {
        this.mCategory = categoryString;
    }

    public void setOnFragmentChangeListener(OnFragmentChangeListener listener) {
        this.mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ll = inflater.inflate(R.layout.fragment_swipe_view, container, false);
        listView = (ListView) ll.findViewById(R.id.lstNewGameSelectTemplate);
        listView.setAdapter(mAdapter);
        TextView empty = (TextView) ll.findViewById(R.id.txtEmpty);
        listView.setEmptyView(empty);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final String selectedName;
                if (mCursor != null) {
                    mCursor.moveToPosition(position);

                    selectedName = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));

                    if (selectedName != null) {

                        // there is a save for this template, ask the user if they want to continue it
                        if (mAdapter.containsSave(selectedName)) {
                            // save exists - ask to resume the last game
                            showResumeGameDialog(position, selectedName);

                        } else {
                            // save doesn't exist - start the puzzle activity
                            startPuzzleActivity(selectedName, pm.getPuzzleData(selectedName));
                        }
                    } else {
                        Log.e("NewGameActivity", "Got a null template name from PuzzleManager");
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
                mActionMode = mode;
                if (mListener != null) mListener.actionModeCreated(mode);
                mode.getMenuInflater().inflate(R.menu.menu_new_game_cab, menu);
                rename = menu.findItem(R.id.menu_cab_rename);
                mAdapter.setMultiSelect(true);
                int count = mAdapter.selectedCount();
                mode.setTitle(count + " Selected");
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

                switch (item.getItemId()) {
                    case (R.id.menu_cab_delete):
                        // delete the puzzle
                        showDeletePuzzleDialog(mode);
                        break;

                    case (R.id.menu_cab_cancel):
                        // cancel action mode
                        mode.finish();
                        break;

                    case (R.id.menu_cab_rename):
                        // rename the selected puzzles
                        showRenameDialog(mode);
                        break;

                    case (R.id.menu_cab_changeCategory):
                        // change the category on the selected puzzles
                        showChangeCategoryDialog(mode);
                        break;

                    case (R.id.menu_cab_deleteSave):
                        // delete the save state on the selected puzzles
                        showDeleteSaveDialog(mode);
                        break;

                    case (R.id.menu_cab_puzzleInfo):
                        // show info on the selected puzzle(1)

                        mCursor.moveToPosition(mAdapter.getSelected().get(0));
                        String puzzleName = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));
                        SudokuUtils.showInfoDialog(getActivity(), pm, puzzleName);
                        break;

                    default:
                        return false;
                }

                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                mAdapter.setMultiSelect(false);
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mAdapter.toggleSelected(position);
                int count = mAdapter.selectedCount();
                mode.setTitle(count + " Selected");

                // disable the rename menu item if there are more than one puzzles selected
                if (count > 1) {
                    rename.setEnabled(false);
                    // when this menu item is hidden, the next is shown, even though its set to never display as icon???
//                    rename.setVisible(false);
                } else if (count == 0) {
                    mode.finish();
                } else {
                    rename.setEnabled(true);
//                    rename.setVisible(true);
                }
            }
        });

        return ll;
    }

    private void showResumeGameDialog(final int cursorPosition, final String selectedName) {
        // ask if the user wants to resume the last saved game for this puzzle
        AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
        ab.setTitle("Resume game?");
        ab.setMessage("Do you want to resume your last game?");
        ab.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mCursor.moveToPosition(cursorPosition);

                // resume the last game
                startPuzzleActivity(selectedName);
                dialog.dismiss();

            }
        }).setNegativeButton("No, delete it", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // delete the last save and start a new game
                pm.deleteSaveState(selectedName);
                startPuzzleActivity(selectedName, pm.getPuzzleData(selectedName));
                dialog.dismiss();

            }
        }).setNeutralButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // bail out
                dialog.cancel();

            }
        }).create().show();
    }

    private void showDeletePuzzleDialog(final ActionMode mode) {
        String message = (mAdapter.getSelected().size() > 1) ?
                String.format(getString(R.string.message_areYouSure_deletePuzzles), mAdapter.getSelected().size()) :
                getString(R.string.message_areYouSure_deletePuzzle);
        // sure you want to delete the puzzle(s)?
        new AlertDialog.Builder(mContext).setTitle(getString(R.string.message_areYouSure)).setMessage(message).setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // yes, delete the puzzle(s)
                String toDelete;
                for (int i : mAdapter.getSelected()) {
                    mCursor.moveToPosition(i);
                    toDelete = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));
                    pm.deletePuzzle(toDelete);
                }
                requeryCursor();
                mAdapter.resetSelected();
                mode.finish();
            }
        }).setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // no don't delete the puzzles
                dialog.cancel();
            }
        }).create().show();
    }

    private void showRenameDialog(final ActionMode mode) {
        mCursor.moveToPosition(mAdapter.getSelected().get(0));
        final String name = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));

        AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
        LinearLayout ll = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.dialog_text_box, null);
        ab.setView(ll);
        final EditText et = (EditText) ll.findViewById(R.id.dialog_text_box);
        et.setHint(name);

        // what should the new name be?
        ab.setTitle(getString(R.string.message_renamePuzzle)).setMessage(getString(R.string.message_enterNewName)).setPositiveButton(getString(R.string.button_rename), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // rename the puzzles
                String newName = et.getText().toString().trim();

                if (pm.containsPuzzle(newName)) {
                    Toast.makeText(getContext(), "That name already exists", Toast.LENGTH_LONG).show();
                    dialog.cancel();
                    return;
                }

                if (name == null || newName.equals(name) || newName.equals("")) return;

                pm.renamePuzzle(name, newName);

                requeryCursor();

                dialog.dismiss();
                mode.finish();
            }
        }).setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // cancel renaming the puzzles
                dialog.cancel();
            }
        });

        ab.create().show();
    }

    private void showChangeCategoryDialog(final ActionMode mode) {
        final String[] items = new String[]{getString(R.string.puzzle_category_easy), getString(R.string.puzzle_category_medium), getString(R.string.puzzle_category_hard)};
        new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.message_selectACategory))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues values = new ContentValues();
                        // change the category to whatever the user clicked
                        values.put(PuzzleManager.PUZZLES_CATEGORY_COL, items[which]);
                        for (int i : mAdapter.getSelected()) {
                            mCursor.moveToPosition(i);
                            String toUpdate = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));
                            pm.getWritableDatabase().update(PuzzleManager.PUZZLES_TABLE, values, PuzzleManager.PUZZLE_NAME_COL + "=?", new String[]{toUpdate});
                        }

                        // ask the mAdapter to force all the fragments to re-query (since we just affected another fragment)
                        // if the listener isn't set up just re-query our cursor
                        if (mListener == null) {
                            Log.e("Fragment", "There is no listener connected from the mAdapter, re-querying self only");
                            requeryCursor();
                        } else {
                            Log.i("Fragment", "Asking mAdapter to re-query all other fragments");
                            mListener.requeryAllFragments();
                        }

                        dialog.dismiss();
                        mode.finish();
                    }
                })
                .create().show();
    }

    private void showDeleteSaveDialog(final ActionMode mode) {
        // are you sure you want to delete these game saves?
        new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.message_areYouSure))
                .setMessage(getString(R.string.message_areYouSure_deleteSaves))
                .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // yes, delete the save states for the selected puzzles
                        for (int i : mAdapter.getSelected()) {
                            mCursor.moveToPosition(i);
                            String puzzleName = mCursor.getString(mCursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));
                            pm.deleteSaveState(puzzleName);
                        }
                        requeryCursor();
                        mode.finish();
                    }
                })
                .setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // no, don't delete them
                        dialog.cancel();
                    }
                })
                .create().show();
    }

    @Override
    public void onResume() {
        Log.d("Fragment", "OnResume");
        super.onResume();

        requeryCursor();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString("DIFFICULTY_FILTER", mCategory);
        outState.putBoolean("MULTI_SELECT", mAdapter.isMultiSelect());
        outState.putIntegerArrayList("SELECTED_ITEMS", mAdapter.getSelected());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            setCategoryFilter(savedInstanceState.getString("DIFFICULTY_FILTER"));
            boolean multiSelect = savedInstanceState.getBoolean("MULTI_SELECT");
            mAdapter.setMultiSelect(multiSelect);
            if(multiSelect) mAdapter.setSelected(savedInstanceState.getIntegerArrayList("SELECTED_ITEMS"));
            if(mActionMode != null) {
                int count = mAdapter.selectedCount();
                mActionMode.setTitle(count + " Selected");
            }

        }

    }

    @Override
    public void onPause() {
        Log.d("Fragment", "OnPause");
        super.onPause();
    }

    private void startPuzzleActivity(String name, int[] puzzle) {
        if (puzzle == null) return;
        Intent intent = new Intent(mContext, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.INTENT_START_TYPE, PuzzleActivity.PUZZLE_FROM_TEMPLATE);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_DATA, puzzle);
        intent.putExtra(PuzzleActivity.INTENT_PUZZLE_NAME, name);
        startActivity(intent);
    }

    private void startPuzzleActivity(String name) {

        Bundle bundle = pm.getSaveState(name);

        Intent intent = new Intent(mContext, PuzzleActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    public interface OnFragmentChangeListener {

        void actionModeCreated(ActionMode actionMode);

        void actionModeDestroyed();

        void requeryAllFragments();

    }
}
