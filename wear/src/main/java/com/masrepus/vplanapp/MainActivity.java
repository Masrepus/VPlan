package com.masrepus.vplanapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vplan_fragment);

        //display vplan data in recylerview
        WearableListView recycler = (WearableListView) findViewById(R.id.vplanlist);
        VplanListAdapter adapter = new VplanListAdapter(this, getVplanList(0));

        recycler.setAdapter(adapter);
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
