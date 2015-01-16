package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.masrepus.vplanapp.constants.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity {

    private int todayVplan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vplan_fragment);

        initTodayVplan();

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        todayVplan = pref.getInt(SharedPrefs.TODAY_VPLAN, 0);

        //request box layout
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
        layout.requestApplyInsets();
        RelativeLayout timestamps = (RelativeLayout) findViewById(R.id.timestamps);
        timestamps.requestApplyInsets();

        //display vplan data in recylerview
        WearableListView listView = (WearableListView) findViewById(R.id.vplanlist);
        VplanListAdapter adapter = new VplanListAdapter(this, getVplanList(todayVplan));

        listView.setAdapter(adapter);

        final TextView header = (TextView) findViewById(R.id.header);
        header.requestApplyInsets();

        header.setText(pref.getString(SharedPrefs.PREF_HEADER_PREFIX + todayVplan, ""));

        //if the list is empty notify the user that there are no items for today
        if (adapter.getItemCount() == 0) {

            TextView noItemsTV = (TextView) findViewById(R.id.noItemsTV);
            noItemsTV.setText(getString(R.string.no_data_today));

            FrameLayout noItemsBg = (FrameLayout) findViewById(R.id.noItemsBg);
            noItemsBg.setVisibility(View.VISIBLE);
        }

        displayTimestamps(pref);
    }

    private void displayTimestamps(SharedPreferences pref) {

        String lastUpdate = pref.getString(SharedPrefs.LAST_UPDATE, "-");
        String timePublished = pref.getString(SharedPrefs.TIME_PUBLISHED_PREFIX + String.valueOf(todayVplan), "");

        //display the timestamps in the textviews
        TextView lastUpdateTV = (TextView) findViewById(R.id.lastUpdateTV);
        lastUpdateTV.setText(getString(R.string.last_update) + " " + lastUpdate);

        TextView timePublishedTV = (TextView) findViewById(R.id.timePublishedTV);
        timePublishedTV.setText(timePublished);
    }

    private void initTodayVplan() {

        SharedPreferences prefs = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //save the position of today's vplan in shared prefs
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("HH");

        for (int i = 0; i < 5; i++) {

            String title = prefs.getString(SharedPrefs.PREF_HEADER_PREFIX + String.valueOf(i), "");
            //skip to the next one if this header doesn't exist
            if (title.contentEquals("")) continue;

            if ((String.valueOf(title)).contains(format.format(calendar.getTime())) && Integer.valueOf(hour.format(calendar.getTime())) < 17) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(SharedPrefs.TODAY_VPLAN, i);
                editor.apply();
            } else if (Integer.valueOf(hour.format(calendar.getTime())) >= 17) {
                calendar = Calendar.getInstance();
                if (calendar.get(Calendar.DAY_OF_WEEK) >= Calendar.FRIDAY) {
                    int daysToMonday = (Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 2) % 7;
                    calendar.add(Calendar.DAY_OF_MONTH, daysToMonday);
                } else calendar.add(Calendar.DAY_OF_MONTH, 1);
                if ((String.valueOf(title)).contains(format.format(calendar.getTime()))) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(SharedPrefs.TODAY_VPLAN, i);
                    editor.apply();
                }
            }
        }
    }

    private ArrayList<Row> getVplanList(int day) {

        VPlanDataSource datasource = new VPlanDataSource(this);
        datasource.open();

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

        datasource.close();
        return rows;
    }
}
