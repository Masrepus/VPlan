package com.masrepus.vplanapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanPagerAdapter extends FragmentStatePagerAdapter {

    Context context;
    private VPlanDataSource datasource;
    private ArrayList<String> filter;

    /**
     * Initialisation of the datasource using the passed context and saving the passed filter arraylist as a local variable
     */
    public VplanPagerAdapter(FragmentManager fm, Context context, ArrayList<String> filter) {
        super(fm);
        datasource = new VPlanDataSource(context);
        this.context = context;
        this.filter = filter;
    }

    /**
     * Requests a new VPlanFragment by passing the current filter and the requested vplan id
     * @param i used as requested vplan id
     */
    @Override
    public Fragment getItem(int i) {

        Fragment fragment = new VplanFragment();
        Bundle args = new Bundle();

        //the id of the requested vplan for this fragment is the fragment's id
        args.putInt(VplanFragment.ARG_REQUESTED_VPLAN_ID, i);
        args.putBoolean(VplanFragment.FLAG_VPLAN_LOADING_DUMMY, false);
        args.putStringArrayList(VplanFragment.ARG_FILTER, filter);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Gets the amount of available days from the db and returns this value
     */
    @Override
    public int getCount() {

        //get the amount of available days from database
        datasource.open();
        Cursor c = datasource.query(MySQLiteHelper.TABLE_LINKS, new String[]{MySQLiteHelper.COLUMN_ID});
        int count = c.getCount();
        datasource.close();

        return count;
    }

    /**
     * Gets the page title from shared prefs by using the position as current vplan id
     */
    @Override
    public CharSequence getPageTitle(int position) {

        CharSequence title;

        //this vplan's current date is used as title
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        title = prefs.getString(MainActivity.PREF_PREFIX_VPLAN_CURR_DATE + String.valueOf(position), "");

        return title;
    }
}
