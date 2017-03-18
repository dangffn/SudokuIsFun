package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Dan on 6/3/2016.
 */
public class PuzzleItemAdapter extends ResourceCursorAdapter {

    ArrayList<Integer> selected;
    ArrayList<String> hasSave;
    boolean selectable = false;
    Bitmap defaultGridBitmap;
    boolean useDefaultIcon;

    public PuzzleItemAdapter(Context context, int layout, Cursor c, int flags, boolean defaultBitmap) {
        super(context, layout, c, flags);
        hasSave = new ArrayList<>();
        selected = new ArrayList<>();
        this.useDefaultIcon = defaultBitmap;

        try {
            InputStream is = context.getAssets().open("default-grid.png");
            defaultGridBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        ImageView icon = (ImageView) view.findViewById(R.id.save_line_item_icon);
        TextView name = (TextView) view.findViewById(R.id.save_line_item_name);
        CheckBox chk = (CheckBox) view.findViewById(R.id.save_line_item_checkbox);
        TextView desc = (TextView) view.findViewById(R.id.save_line_item_desc);
        ImageView btnInfo = (ImageView) view.findViewById(R.id.btnPuzzleInfo);

        // set icon, name, and description
        final String puzzleName = cursor.getString(cursor.getColumnIndex(PuzzleManager.PUZZLE_NAME_COL));
        String description = cursor.getString(cursor.getColumnIndex(PuzzleManager.PUZZLE_DESC_COL));
        boolean puzzleSaved = cursor.getInt(cursor.getColumnIndex(PuzzleManager.SAVED_COL)) == PuzzleManager.YES;
        boolean puzzleFinished = puzzleSaved && cursor.getInt(cursor.getColumnIndex(PuzzleManager.SAVE_FINISHED)) == PuzzleManager.YES;
        byte[] blob = (puzzleSaved) ?
                cursor.getBlob(cursor.getColumnIndex(PuzzleManager.SAVE_ICON)) :
                cursor.getBlob(cursor.getColumnIndex(PuzzleManager.PUZZLE_ICON_COL));

        if(puzzleSaved) {
            if(!hasSave.contains(puzzleName)) {
                hasSave.add(puzzleName);
            }

        } else {
            hasSave.remove(puzzleName);
        }

        if(useDefaultIcon) {
            // user wants to use the default grid bitmap to save space
            icon.setImageBitmap(defaultGridBitmap);

        } else {
            if (blob != null) {
                // user wants to use the saved icon
                icon.setImageBitmap(BitmapFactory.decodeByteArray(blob, 0, blob.length));

            } else {
                // user wants to use the saved icon but its null, so use the default icon
                icon.setImageBitmap(defaultGridBitmap);
            }
        }

        name.setText(puzzleName);
        if (puzzleSaved) {
            if(puzzleFinished) {
                // add finished to description
                desc.setText(String.format(context.getString(R.string.message_finished), description));
                view.setBackgroundResource(R.drawable.puzzle_item_background_finished);
            } else {
                // add in progress to description
                desc.setText(String.format(context.getString(R.string.message_inProgress), description));
                view.setBackgroundResource(R.drawable.puzzle_item_background_saved);
            }
        } else {
            // add normal description
            desc.setText(description);
            view.setBackgroundResource(R.drawable.puzzle_item_background_normal);
        }


        // set card background and checkbox status
        if (selectable) {
            // selectable mode is on
            icon.setVisibility(View.GONE);
            chk.setVisibility(View.VISIBLE);
            if (selected.contains(cursor.getPosition())) {
                // this puzzle is selected in action mode
                chk.setChecked(true);
                view.setBackgroundResource(R.drawable.puzzle_item_background_selected);
            }

        } else {
            // selectable mode is off
            icon.setVisibility(View.VISIBLE);
            chk.setVisibility(View.GONE);
            chk.setChecked(false);
        }

        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SudokuUtils.showInfoDialog(context, PuzzleManager.getInstance(context), puzzleName);
            }
        });
    }

    public void toggleSelected(int position) {
        if (!selected.contains(position)) {
            selected.add(position);
        } else {
            selected.remove(Integer.valueOf(position));
        }
        notifyDataSetChanged();
    }

    public void resetSelected() {
        selected = new ArrayList<>();
    }

    public void setMultiSelect(boolean set) {
        selectable = set;
        if (!set) resetSelected();
        notifyDataSetChanged();
    }

    public boolean isMultiSelect() {
        return selectable;
    }

    public ArrayList<Integer> getSelected() {
        return selected;
    }

    public void setSelected(ArrayList<Integer> selected) {
        this.selected = selected;
    }

    public int selectedCount() {
        return selected.size();
    }

    public boolean containsSave(String name) {
        return hasSave.contains(name);
    }
}