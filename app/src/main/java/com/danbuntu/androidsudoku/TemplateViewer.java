package com.danbuntu.androidsudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by Dan on 6/4/2016.
 */
public class TemplateViewer extends AlertDialog.Builder {

    Context context;
    Cursor cursor;
    OnTemplateSelectedListener listener;

    public TemplateViewer(Context context) {
        this(context, -1);
    }

    public TemplateViewer(Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    public void setOnTemplateSelectedListener(OnTemplateSelectedListener listener) {
        this.listener = listener;
    }

    public void init(final Cursor cursor) {
        if (cursor == null) return;

        this.cursor = cursor;

        setTitle("Select a Template");

        ListView ll = new ListView(context);
        ll.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.MATCH_PARENT));
        CustomCursorAdapter adapter = new CustomCursorAdapter(context, R.layout.list_item_puzzle_save, cursor, -1);
        ll.setAdapter(adapter);

        ll.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listener != null) {
                    cursor.moveToPosition(position);
//                    byte[] blob = cursor.getBlob(cursor.getColumnIndex(TemplateManager.TEMPLATE_DATA_COL));
//                    int[] out = new int[blob.length];
//                    for(int i=0; i<blob.length; i++)
//                        out[i] = (int)blob[i] & 0xff;
//                    listener.templateSelected(out);
                    listener.templateSelected(cursor.getString(cursor.getColumnIndex(TemplateManager.NAME_COL)));
                }
            }
        });

        setView(ll);

    }

    public interface OnTemplateSelectedListener {
        void templateSelected(String templateName);
    }
}
