package com.masrepus.vplanapp;

import android.app.Activity;
import android.os.Bundle;
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
        //((TextView) findViewById(R.id.title)).setText("Dienstag, 30.12.2014");
        ArrayList<Row> rows = new ArrayList<>();
        rows.add(new Row("1", "10A", "entfällt"));
        rows.add(new Row("2", "10A", "entfällt"));
        rows.add(new Row("3", "10A", "entfällt"));
        rows.add(new Row("4", "10A", "entfällt"));
        rows.add(new Row("5", "10A", "entfällt"));
        rows.add(new Row("6", "10A", "entfällt"));
        rows.add(new Row("7", "10A", "entfällt"));

        /*VplanListAdapter adapter = new VplanListAdapter(this, rows);
        WearableListView listView = (WearableListView) findViewById(R.id.vplanlist);
        listView.setAdapter(adapter);*/
    }
}
