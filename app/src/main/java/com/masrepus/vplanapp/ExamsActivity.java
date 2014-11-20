package com.masrepus.vplanapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class ExamsActivity extends ActionBarActivity {

    private ArrayList<ExamsRow> examsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exams_activity);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        refreshAdapter();
    }

    public void refreshAdapter() {
        //set listadapter
        examsList = initData();
        ListView listView = (ListView) findViewById(R.id.examsList);
        ExamsListAdapter adapter = new ExamsListAdapter(this, R.layout.exam_list_element, examsList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.exams, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {

            case R.id.action_refresh:
                new BgDownloader().execute(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<ExamsRow> initData() {

        VPlanDataSource datasource = new VPlanDataSource(this);
        datasource.open();

        ArrayList<ExamsRow> examsList = new ArrayList<ExamsRow>();

        Cursor c = datasource.query(MySQLiteHelper.TABLE_TESTS, new String[]{MySQLiteHelper.COLUMN_DATE, MySQLiteHelper.COLUMN_KLASSE});

        //fill arraylist
        while (c.moveToNext()) {
            String date = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_DATE));
            String subject = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_KLASSE));

            ExamsRow row = new ExamsRow(date, subject);
            examsList.add(row);
        }

        datasource.close();

        return examsList;
    }

    private class ExamsListAdapter extends ArrayAdapter<ExamsRow> {

        private ArrayList<ExamsRow> examsList;
        private Context context;

        public ExamsListAdapter(Context context, int resource, List<ExamsRow> examsList) {
            super(context, resource, examsList);
            this.examsList = (ArrayList<ExamsRow>) examsList;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder view;
            final ExamsRow row = examsList.get(position);

            if (row != null) {

                //check for an existing viewholder
                if (convertView == null) {
                    view = new ViewHolder();
                    convertView = View.inflate(context, R.layout.exam_list_element, null);
                    view.date = (TextView) convertView.findViewById(R.id.date);
                    view.subject = (TextView) convertView.findViewById(R.id.subject);
                } else view = (ViewHolder) convertView.getTag(R.id.TAG_VIEWHOLDER);

                view.date.setText(row.getDate());
                view.subject.setText(row.getSubject());

                convertView.setTag(R.id.TAG_VIEWHOLDER, view);
                convertView.setTag(R.id.TAG_DATE, row.getDate());
                convertView.setTag(R.id.TAG_SUBJECT, row.getSubject());
            }

            return convertView;
        }

        protected class ViewHolder {
            protected TextView date;
            protected TextView subject;
        }
    }

    private class BgDownloader extends AsyncDownloader {

        @Override
        protected int getAppMode() {
            return MainActivity.TESTS;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            refreshAdapter();
        }
    }
}
