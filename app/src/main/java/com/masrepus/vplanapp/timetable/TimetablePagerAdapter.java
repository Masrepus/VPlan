package com.masrepus.vplanapp.timetable;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperTimetable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetablePagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<TimetableListAdapter> adapters = new ArrayList<>();
    private SimpleDateFormat weekdays = new SimpleDateFormat("EEEE");
    private Calendar calendar;
    private int[] daysOfWeek = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};
    private TimetableActivity activity;


    public TimetablePagerAdapter(TimetableActivity activity, FragmentManager fm) {
        super(fm);
        calendar = Calendar.getInstance();
        this.activity = activity;

        DataSource datasource = new DataSource(activity);
        datasource.open();

        //create the adapters for the fragments and store them here
        for (int i = 0; i < 5; i++) {

            ArrayList<TimetableRow> tempRows = new ArrayList<>();
            Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.DAYS[i], new String[]{SQLiteHelperTimetable.COLUMN_LESSON, SQLiteHelperTimetable.COLUMN_SUBJECT, SQLiteHelperTimetable.COLUMN_ROOM});

            //iterate through all of this day's timetable entries and add them to the arraylist
            while (c.moveToNext()) {

                String lesson = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_LESSON));
                String subject = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_SUBJECT));
                String room = c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_ROOM));

                tempRows.add(new TimetableRow(lesson, subject, room));
            }

            //transfer those lessons into the final list and add free lessons where needed
            ArrayList<TimetableRow> rows = new ArrayList<>();

            Collections.sort(tempRows, new Comparator<TimetableRow>() {
                @Override
                public int compare(TimetableRow lhs, TimetableRow rhs) {
                    //sort by lessons
                    return Integer.valueOf(lhs.getLesson()).compareTo(Integer.valueOf(rhs.getLesson()));
                }
            });

            tempRows = fillGaps(tempRows);

            int maxLesson = Integer.valueOf(tempRows.get(tempRows.size()-1).getLesson());
            //check if the last lesson is lesson 6
            if (maxLesson <= 6) {

                //fill the list up with free lessons until there are 6 lessons available
                tempRows = fillListUp(tempRows, 6);

            } else if (maxLesson <= 8) /*two afternoon lessons*/ {

                //fill the list up until there are 8 lessons available
                tempRows = fillListUp(tempRows, 8);

            } else tempRows = fillListUp(tempRows, 10); //four afternoon lessons

            //sort the rows one last time
            Collections.sort(tempRows, new Comparator<TimetableRow>() {
                @Override
                public int compare(TimetableRow lhs, TimetableRow rhs) {
                    //sort by lessons
                    return Integer.valueOf(lhs.getLesson()).compareTo(Integer.valueOf(rhs.getLesson()));
                }
            });

            insertBreaks(tempRows, rows, maxLesson);

            //now build a new adapter for this weekday
            adapters.add(new TimetableListAdapter(activity, rows));
        }

        datasource.close();
    }

    private ArrayList<TimetableRow> insertBreaks(ArrayList<TimetableRow> tempRows, ArrayList<TimetableRow> rows, int maxLesson) {

        //now insert the break items
        int tempRowsId = 0;
        for (int lesson = 1; lesson <= maxLesson; lesson++) {

            if (lesson == 7) continue;

            rows.add(tempRows.get(tempRowsId));

            //now add a break every two lessons
            switch (lesson) {

                case 6:
                    if (maxLesson != 6) {
                        rows.add(new BreakRow());
                    }
                    break;
                case 9:
                    if (maxLesson != 9) {
                        rows.add(new BreakRow());
                    }
                    break;
                case 2:
                case 4:
                    rows.add(new BreakRow());
                    break;
            }

            tempRowsId++;
        }

        return rows;
    }

    private ArrayList<TimetableRow> fillListUp(ArrayList<TimetableRow> list, int maxLesson) {

        int lastLesson = Integer.valueOf(list.get(list.size()-1).getLesson());

        while (lastLesson < maxLesson) {

            list.add(new TimetableRow(String.valueOf(lastLesson + 1), activity.getString(R.string.free_lesson), ""));
            lastLesson++;
        }

        return list;
    }

    private ArrayList<TimetableRow> fillGaps(ArrayList<TimetableRow> list) {

        int position = 0;

        ArrayList<TimetableRow> cache = new ArrayList<>();

        for (int lesson = 1; lesson <= Integer.valueOf(list.get(list.size()-1).getLesson()); lesson++) {

            if (position > list.size() - 1) {

                //add a free lesson here
                cache.add(new TimetableRow(String.valueOf(lesson), activity.getString(R.string.free_lesson), ""));
            } else {

                if (lesson == 7) continue; //lesson 7 is no valid lesson

                if (!(Integer.valueOf(list.get(position).getLesson()) == lesson)) {

                    //add a free lesson here
                    cache.add(position, new TimetableRow(String.valueOf(lesson), activity.getString(R.string.free_lesson), ""));
                } else {

                    //add the lesson that was saved
                    cache.add(list.get(position));
                    position++;
                }
            }
        }

        return cache;
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
