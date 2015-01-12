package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.masrepus.vplanapp.constants.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vplan_fragment);

        //display vplan data in recylerview
        WearableListView listView = (WearableListView) findViewById(R.id.vplanlist);
        VplanListAdapter adapter = new VplanListAdapter(this, getVplanList(1));

        listView.setAdapter(adapter);

        final TextView header = (TextView) findViewById(R.id.header);
        header.requestApplyInsets();
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        header.setText(pref.getString(SharedPrefs.PREF_HEADER_PREFIX + 1, "Fehler"));
    }

    private void getTodayVplan() {

        SharedPreferences prefs = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //save the position of today's vplan in shared prefs
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("hh");

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
