package com.masrepus.vplanapp.timetable;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.NumberPicker;

/**
 * Created by samuel on 25.02.15.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)//For backward-compability
public class MyNumberPicker extends NumberPicker {

    public MyNumberPicker(Context context) {
        super(context);
        setMinValue(1);
        setMaxValue(10);
        this.setDisplayedValues(new String[]{"1", "2", "3", "4", "5", "6", "8", "9", "10", "11"});
    }

    public MyNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttributeSet(attrs);
        setMinValue(1);
        setMaxValue(10);
        this.setDisplayedValues(new String[]{"1", "2", "3", "4", "5", "6", "8", "9", "10", "11"});
    }

    public MyNumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        processAttributeSet(attrs);
        setMinValue(1);
        setMaxValue(10);
        this.setDisplayedValues(new String[]{"1", "2", "3", "4", "5", "6", "8", "9", "10", "11"});
    }

    private void processAttributeSet(AttributeSet attrs) {
    }

    public int getLesson() {

        //handle the missing 7
        int value = getValue();
        if (value <= 6) return value;
        else return value + 1;
    }

    public void setLesson(int lesson) {

        if (lesson <= 6) setValue(lesson);
        else if (lesson == 7) setValue(7);
        else setValue(lesson - 1);
    }
}
