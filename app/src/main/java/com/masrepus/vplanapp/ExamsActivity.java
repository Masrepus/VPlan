package com.masrepus.vplanapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class ExamsActivity extends ActionBarActivity {

    public static final String PREF_HIDE_OLD_EXAMS = "hideOldExams";

    private ArrayList<ExamsRow> examsList;
    private MenuItem refreshItem;
    private boolean noOldItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exams_activity);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setSubtitle(getString(R.string.exams_activity_subtitle));

        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        noOldItems = pref.getBoolean(PREF_HIDE_OLD_EXAMS, false);

        //hide or show hidden items info
        FrameLayout hiddenItemsFL = (FrameLayout) findViewById(R.id.frameLayout2);
        if (noOldItems) hiddenItemsFL.setVisibility(View.VISIBLE);
        else hiddenItemsFL.setVisibility(View.GONE);

        refreshAdapter();
    }

    public void refreshAdapter() {
        //set listadapter
        examsList = initData();
        //sort by date
        Collections.sort(examsList, new Comparator<ExamsRow>() {
            @Override
            public int compare(ExamsRow examsRow, ExamsRow examsRow2) {
                return examsRow.getDate().compareTo(examsRow2.getDate());
            }
        });

        ListView listView = (ListView) findViewById(R.id.examsList);
        ExamsListAdapter adapter = new ExamsListAdapter(this, R.layout.exam_list_element, examsList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.exams, menu);

        //init the filter item
        MenuItem filterItem = (MenuItem) menu.findItem(R.id.action_activate_filter);
        filterItem.setChecked(noOldItems);

        return true;
    }

    @Override
    protected void onPause() {
        //save the filter state
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_HIDE_OLD_EXAMS, noOldItems);
        editor.apply();

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {

            case R.id.action_refresh:
                refresh(item);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_open_browser:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.open_browser) + "...")
                        .setItems(new CharSequence[]{getString(R.string.unterstufe) + "/" + getString(R.string.mittelstufe), getString(R.string.oberstufe)}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //open the right website
                                switch (i) {
                                    case 0:
                                        //u/minfo
                                        Uri uri = Uri.parse(AsyncDownloader.findRequestedTestsPage(getApplicationContext(), MainActivity.MINFO));
                                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                        break;
                                    case 1:
                                        //oinfo
                                        uri = Uri.parse(AsyncDownloader.findRequestedTestsPage(getApplicationContext(), MainActivity.OINFO));
                                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                        break;
                                }
                            }
                        })
                        .show();
                return true;
            case R.id.action_activate_filter:
                item.setChecked(!item.isChecked());

                noOldItems = item.isChecked();

                //hide or show hidden items info
                FrameLayout hiddenItemsFL = (FrameLayout) findViewById(R.id.frameLayout2);
                if (noOldItems) hiddenItemsFL.setVisibility(View.VISIBLE);
                else hiddenItemsFL.setVisibility(View.GONE);

                refreshAdapter();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<ExamsRow> initData() {

        VPlanDataSource datasource = new VPlanDataSource(this);
        datasource.open();

        ArrayList<ExamsRow> examsList = new ArrayList<ExamsRow>();

        //get the data from both test tables
        Cursor c = datasource.query(SQLiteHelperTests.TABLE_TESTS_UINFO_MINFO, new String[]{SQLiteHelperTests.COLUMN_DATE, SQLiteHelperTests.COLUMN_GRADE, SQLiteHelperTests.COLUMN_TYPE, SQLiteHelperTests.COLUMN_SUBJECT});

        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

        //fill arraylist
        while (c.moveToNext()) {

            String date = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_DATE));

            //if requested, skip old entries
            if (noOldItems) {
                Date currDate;
                try {
                    currDate = format.parse(date);
                } catch (ParseException e) {
                    currDate = null;
                }
                if (currDate != null) {
                    if (currDate.before(today)) continue;
                }
            }

            String subject = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_SUBJECT));
            String type = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_TYPE));
            String grade = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_GRADE));

            ExamsRow row = new ExamsRow(date, grade, subject, type);
            examsList.add(row);
        }

        c = datasource.query(SQLiteHelperTests.TABLE_TESTS_OINFO, new String[]{SQLiteHelperTests.COLUMN_DATE, SQLiteHelperTests.COLUMN_GRADE, SQLiteHelperTests.COLUMN_TYPE, SQLiteHelperTests.COLUMN_SUBJECT});

        while (c.moveToNext()) {

            String date = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_DATE));

            //if requested, skip old entries
            if (noOldItems) {
                Date currDate;
                try {
                    currDate = format.parse(date);
                } catch (ParseException e) {
                    currDate = null;
                }
                if (currDate != null) {
                    if (currDate.before(today)) continue;
                }
            }
            
            String subject = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_SUBJECT));
            String type = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_TYPE));
            String grade = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_GRADE));

            ExamsRow row = new ExamsRow(date, grade, subject, type);
            examsList.add(row);
        }

        return examsList;
    }

    private void refresh(MenuItem item) {

        //rotate the refresh button
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

        Animation rotation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.refresh_clockwise);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        refreshItem = item;

        new BgDownloader().execute(this);
    }

    public void resetRefreshAnimation() {

        //reset the refresh button to normal, non-rotating layout (if it has been initialised yet)
        if (refreshItem != null) {
            if (refreshItem.getActionView() != null) {
                refreshItem.getActionView().clearAnimation();
                refreshItem.setActionView(null);
            }
        }
    }

    public void showAlert(final Context context, int titleStringRes, int msgStringRes, int buttonCount) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleStringRes)
                .setMessage(msgStringRes);

        //if buttoncount is 1, then there should be only one ok button, otherwise also add a cancel one
        switch (buttonCount) {
            case 1:
                builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                break;
            case 2:
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(context, SettingsActivity.class), 0);
                    }
                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                break;
        }

        builder.show();
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
                    view.type = (TextView) convertView.findViewById(R.id.type);
                    view.grade = (TextView) convertView.findViewById(R.id.grade);
                } else view = (ViewHolder) convertView.getTag(R.id.TAG_VIEWHOLDER);

                view.date.setText(row.getDateString());
                view.subject.setText(row.getSubject());

                //check whether this type is longer than four characters and if needed shorten it
                if (row.getType().toCharArray().length > 4) {
                    String shortType = "";
                    char[] chars = row.getType().toCharArray();
                    for (int i=0; i<2; i++) {
                        shortType += chars[i];
                    }
                    //at the end add ...
                    shortType += "…";
                    view.type.setText(shortType);
                    convertView.setTag(R.id.TAG_TYPE_FULL, row.getType());
                }
                view.type.setText(row.getType());

                view.grade.setText(row.getGrade());

                convertView.setTag(R.id.TAG_VIEWHOLDER, view);
                convertView.setTag(R.id.TAG_DATE, row.getDateString());
                convertView.setTag(R.id.TAG_SUBJECT, row.getSubject());
            }

            return convertView;
        }

        protected class ViewHolder {
            protected TextView date;
            protected TextView subject;
            protected TextView type;
            protected TextView grade;
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

            progressBar.setVisibility(View.GONE);

            resetRefreshAnimation();

            refreshAdapter();
        }

        @Override
        protected void onProgressUpdate(Enum... values) {
            super.onProgressUpdate(values);

            switch ((ProgressCode) values[0]) {
                case STARTED:
                    progress = (ProgressCode) values[0];
                    progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                    break;
                case PARSING_FINISHED:
                    progress = (ProgressCode) values[0];
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress((int) (100 * (double) downloaded / total_downloads));
                    break;
                case ERR_NO_CREDS:
                    progress = (ProgressCode) values[0];
                    progressBar.setVisibility(View.GONE);

                    showAlert(context, R.string.no_creds, R.string.download_error_nocreds, 2);

                    resetRefreshAnimation();

                    break;
                case ERR_NO_INTERNET:
                    progress = (ProgressCode) values[0];
                    progressBar.setVisibility(View.GONE);

                    showAlert(context, R.string.download_error_title, R.string.download_error_nointernet, 1);

                    resetRefreshAnimation();

                    break;
                case ERR_NO_INTERNET_OR_NO_CREDS:
                    progress = (ProgressCode) values[0];
                    progressBar.setVisibility(View.GONE);

                    showAlert(context, R.string.download_error_title, R.string.download_error, 1);

                    resetRefreshAnimation();

                    break;
                case NOTHING_TO_DOWNLOAD:
                    progress = (ProgressCode) values[0];
                    progressBar.setVisibility(View.GONE);

                    showAlert(context, R.string.nothing_to_download, R.string.nothing_to_download_msg, 2);
            }
        }
    }
}
