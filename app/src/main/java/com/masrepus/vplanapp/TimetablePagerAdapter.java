package com.masrepus.vplanapp;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.masrepus.vplanapp.constants.Args;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetablePagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<TimetableListAdapter> adapters = new ArrayList<>();
    private SimpleDateFormat weekdays = new SimpleDateFormat("EEEE");
    private Calendar calendar;
    private int[] daysOfWeek = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};


    public TimetablePagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        calendar = Calendar.getInstance();

        DataSource datasource = new DataSource(context);
        datasource.open();

        //create the adapters for the fragments and store them here
        for (int i = 0; i < 5; i++) {

            ArrayList<TimetableRow> rows = new ArrayList<>();
            Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.DAYS[i], new String[]{SQLiteHelperTimetable.COLUMN_LESSON, SQLiteHelperTimetable.COLUMN_SUBJECT, SQLiteHelperTimetable.COLUMN_ROOM});

            //iterate through all of this day's timetable entries and add them to the arraylist
            while (c.moveToNext()) {

                String lesson = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_LESSON));
                String subject = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_SUBJECT));
                String room = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_ROOM));

                rows.add(new TimetableRow(lesson, subject, room));
            }

            //now build a new adapter for this weekday
            adapters.add(new TimetableListAdapter(context, rows));
        }
    }

    @Override
    public Fragment getItem(int position) {

        TimetableFragment fragment = new TimetableFragment();

        //attach the listview adapter for this day
        Bundle args = new Bundle();
        args.putSerializable(Args.ADAPTER, adapters.get(position));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getCount() {
        //this is always the same: 5 weekdays
        return 5;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //return the localised form of this day's weekday

        //set to this fragment's day of week
        calendar.set(Calendar.DAY_OF_WEEK, daysOfWeek[position]);
        return weekdays.format(calendar.getTime());
    }
}
