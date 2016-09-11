package com.danbuntu.androidsudoku;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Dan on 6/3/2016.
 */
public class CustomCursorAdapter extends ResourceCursorAdapter {

    ArrayList<String> hasSave;
    ArrayList<Integer> selected;
    boolean selectable = false;

    public CustomCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        hasSave = new ArrayList<>();
        selected = new ArrayList<>();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ImageView icon = (ImageView) view.findViewById(R.id.save_line_item_icon);

        String n = cursor.getString(cursor.getColumnIndex(TemplateManager.NAME_COL));
        TextView name = (TextView) view.findViewById(R.id.save_line_item_name);

        CheckBox chk = (CheckBox) view.findViewById(R.id.save_line_item_checkbox);

        if(cursor.getInt(cursor.getColumnIndex(TemplateManager.SAVED_COL)) == TemplateManager.HAS_SAVE) {
            if(!hasSave.contains(n)) hasSave.add(n);

            name.setText(n + " *SAVED*");
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(TemplateManager.SAVE_ICON));
            if(blob != null)
                icon.setImageBitmap(BitmapFactory.decodeByteArray(blob, 0, blob.length));

        } else {
            if(hasSave.contains(n)) hasSave.remove(n);

            name.setText(n);
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(TemplateManager.TEMPLATE_ICON));
            if(blob != null)
                icon.setImageBitmap(BitmapFactory.decodeByteArray(blob, 0, blob.length));
        }

        if(selectable) {
            icon.setVisibility(View.GONE);
            chk.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.VISIBLE);
            chk.setVisibility(View.GONE);
        }

        CardView card = (CardView) view.findViewById(R.id.cardView);
        if (selected.contains(cursor.getPosition())) {
            chk.setChecked(true);
            card.setBackgroundColor(context.getResources().getColor(R.color.light_blue));
        } else {
            chk.setChecked(false);
            card.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView desc = (TextView) view.findViewById(R.id.save_line_item_desc);
        desc.setText(cursor.getString(cursor.getColumnIndex(TemplateManager.DESC_COL)));

    }

    public void setSaves(ArrayList<String> saves) {
        if(saves != null)
            hasSave.addAll(saves);
    }

    public void toggleSelected(int position) {
        if(!selected.contains(position)) {
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
        if(!set) resetSelected();
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getSelected() {
        return selected;
    }

    public int selectedCount() {
        return selected.size();
    }

    public boolean containsSave(String name) {
        return hasSave.contains(name);
    }
}