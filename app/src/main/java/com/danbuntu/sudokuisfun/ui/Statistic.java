package com.danbuntu.sudokuisfun.ui;

import com.danbuntu.sudokuisfun.utils.SudokuUtils;

/**
 * Created by dan on 7/15/16. Have a great day!
 */
public class Statistic {
    private String secondaryValue = null;
    private String desc = null;
    private String value = null;

    public Statistic(String description, Object value, Object secondaryValue) {
        this.desc = description;
        this.value = resolveValue(value);
        if(this.value != null) this.secondaryValue = resolveValue(secondaryValue);
    }

    private String resolveValue(Object value) {
        if(value == null || (!(value instanceof String) && value.toString().equals("0"))) {
            return null;
        }

        if(value instanceof String) {
            return value.toString();

        } else if(value instanceof Long) {
            return SudokuUtils.getReadableDuration((long) value);

        } else if(value instanceof Integer) {
            return SudokuUtils.formatTousandths((int) value);

        } else {
            return null;
        }
    }

    public void setValue(Object value) {
        this.value = resolveValue(value);
    }

    public String getDescription() {
        return this.desc;
    }

    public String getPrimaryValue() {
        return this.value;
    }

    public String getSecondaryValue() {
        return this.secondaryValue;
    }
}
