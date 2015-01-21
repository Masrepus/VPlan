package com.masrepus.vplanapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;

import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.util.ArrayList;

/**
 * Created by samuel on 16.01.15.
 */
public class VplanPagerAdapter extends FragmentGridPagerAdapter {

    private int days;
    private Context context;
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private VPlanDataSource datasource;

    public VplanPagerAdapter(FragmentManager fm, Context context) {
        super(fm);

        this.context = context;

        datasource = new VPlanDataSource(context);
        datasource.open();

        //get the number of available days and save it for the column number
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        days = pref.getInt(SharedPrefs.DAYS_COUNT, 0);

        //check if there is data available and accordingly add column fragments to the list
        if (days > 0) {

            for (int i = 0; i < days; i++) {

                //create a new VplanFragment and pass the currently requested day as an argument
                VplanFragment fragment = new VplanFragment();
                Bundle args = new Bundle();
                args.putInt(Args.ARG_POSITION, i);
                args.putSerializable(Args.VPLAN_LIST, getVplanList(i));
                fragment.setArguments(args);

                //add it to the list
                fragments.add(fragment);
            }
        } else fragments.add(new EmptyFragment());

        datasource.close();
    }

    @Override
    public Fragment getFragment(int col, int day) {

        //return the correct fragment from the list
        return fragments.get(day);
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {

        //return at least 1
        return (days > 0) ? days : 1;
    }

    private ArrayList<Row> getVplanList(int day) {

        Cursor c = datasource.query(SQLiteHelperVplan.tablesVplan[day], new String[]{SQLiteHelperVplan.COLUMN_GRADE, SQLiteHelperVplan.COLUMN_STATUS, SQLiteHelperVplan.COLUMN_STUNDE});

        //build the arraylist
        ArrayList<Row> rows = new ArrayList<>();

        while (c.moveToNext()) {

            Row row = new Row();
            row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
            row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));
            row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));

            rows.add(row);
        }

        return rows;
    }
}
