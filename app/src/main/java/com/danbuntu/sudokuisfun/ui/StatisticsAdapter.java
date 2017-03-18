package com.danbuntu.sudokuisfun.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.danbuntu.sudokuisfun.R;

/**
 * Created by dan on 7/28/16. Have a great day!
 */
public class StatisticsAdapter extends ArrayAdapter {

    final int mLayoutRes;
    boolean showSecondaryValues;
    Context mContext;

    public StatisticsAdapter(Context context, int layoutRes) {
        this(context, layoutRes, true);
    }

    public StatisticsAdapter(Context context, int layoutRes, boolean showSecondaryValues) {
        super(context, layoutRes);
        this.mLayoutRes = layoutRes;
        this.mContext = context;
        this.showSecondaryValues = showSecondaryValues;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder = new ViewHolder();

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayoutRes, null);
        }

        viewHolder.description = (TextView) convertView.findViewById(R.id.listItemStatDescription);
        viewHolder.value = (TextView) convertView.findViewById(R.id.listItemStatValue);
        viewHolder.secondaryValue = (TextView) convertView.findViewById(R.id.listItemStatSecondaryValue);

        // hide the third view if we aren't using it
        if (!showSecondaryValues) viewHolder.secondaryValue.setVisibility(View.GONE);

        Statistic currentStat = (Statistic) getItem(position);
        viewHolder.description.setText(currentStat.getDescription());
        String currentValue = currentStat.getPrimaryValue();
        String secondaryValue = currentStat.getSecondaryValue();

        if (currentValue != null) {
            viewHolder.value.setText(currentValue);
            viewHolder.value.setTextColor(mContext.getResources().getColor(R.color.text_color_description));
            viewHolder.secondaryValue.setText(secondaryValue);
        } else {
            viewHolder.value.setText(mContext.getString(R.string.message_noData));
            viewHolder.value.setTextColor(mContext.getResources().getColor(R.color.text_color_description_secondary));
            viewHolder.secondaryValue.setText(null);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView description;
        TextView value;
        TextView secondaryValue;
    }
}
