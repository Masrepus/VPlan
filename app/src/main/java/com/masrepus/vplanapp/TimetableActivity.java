package com.masrepus.vplanapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class TimetableActivity extends ActionBarActivity implements View.OnClickListener, DialogInterface.OnClickListener, View.OnLongClickListener, Serializable {

    private SimpleDateFormat weekdays = new SimpleDateFormat("EEEE");
    private View dialogView;
    private TimetableRow editingRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timetable_activity);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //display a loading view
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabStrip.setVisibility(View.INVISIBLE);

        new PagerAdapterLoader().execute();
    }

    private void initPager(TimetablePagerAdapter adapter) {

        //prepare the pager
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        //scroll to today's page
        Calendar calendar = Calendar.getInstance();
        String today = weekdays.format(calendar.getTime());
        int todayPosition = 0;

        for (int i = 0; i < 5; i++) {
            if (today.contentEquals(adapter.getPageTitle(i))) todayPosition = i;
        }
        pager.setCurrentItem(todayPosition);

        //prepare the title strip
        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabStrip.setTabIndicatorColor(getResources().getColor(R.color.blue));


        //set a 1 dp margin between the fragments
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        pager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));

        pager.setOffscreenPageLimit(5);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //set the appmode to timetable
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SharedPrefs.APPMODE, AppModes.TIMETABLE);
        editor.apply();

        prepareDrawer();
    }

    @Override
    protected void onPause() {

        //set the appmode to vplan
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SharedPrefs.APPMODE, AppModes.VPLAN);
        editor.apply();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timetable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case android.R.id.home:
                //update appmode
                SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                pref.edit().putInt(SharedPrefs.APPMODE, AppModes.VPLAN).apply();
                return super.onOptionsItemSelected(item);
            case R.id.action_add:
                //add a lesson to the currently visible day
                showAddLessonDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAddLessonDialog() {

        //now show the user a dialog where he can add a new lesson
        dialogView = View.inflate(this, R.layout.add_lesson_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_lesson))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .show();
    }

    private void addLesson(DataSource datasource, int day, int lesson, String subject, String room) {

        datasource.open();

        //save the lesson details in the timetable db, if it doesn't exist already
        String tableName = SQLiteHelperTimetable.DAYS[day];

        Cursor c = datasource.queryTimetable(tableName, new String[]{SQLiteHelperTimetable.COLUMN_LESSON}, SQLiteHelperTimetable.COLUMN_LESSON + "='" + lesson + "'");
        if (c.getCount() == 0) datasource.createRowTimetable(tableName, String.valueOf(lesson), String.valueOf(subject), room);
        else Toast.makeText(this, getString(R.string.lesson_already_saved), Toast.LENGTH_SHORT).show();

        datasource.close();
    }

    private void prepareDrawer() {

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //initialise the drawer list
        DrawerListBuilder builder = new DrawerListBuilder(this, getResources().getStringArray(R.array.sectionHeaders), getResources().getStringArray(R.array.appmodes), 0);
        DrawerListAdapter adapter = new DrawerListAdapter(this, this, builder.getItems());
        ListView drawerLV = (ListView) findViewById(R.id.vplanModeList);
        drawerLV.setAdapter(adapter);

        //save the current appmode item that is selected (tests)
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SharedPrefs.SELECTED_APPMODE_ITEM, 1 + AppModes.TIMETABLE);
        editor.apply();

        adapter.notifyDataSetChanged();

        //display last update timestamp
        String lastUpdate = pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + AppModes.TESTS, "");
        TextView tv = (TextView) findViewById(R.id.lastUpdate);
        tv.setVisibility(View.VISIBLE);
        tv.setText(lastUpdate);

        //display current app info in appinfo textview
        TextView appInfo = (TextView) findViewById(R.id.textViewAppInfo);
        try {
            appInfo.setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " by Samuel Hopstock");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appInfo.setText("Fehler");
        }

        //init the settings item
        TextView settings = (TextView) findViewById(R.id.textViewSettings);
        settings.setText(getString(R.string.settings).toUpperCase());
    }

    @Override
    public void onClick(View view) {

        //check the tag
        Integer appModeTag = (Integer) view.getTag(R.id.TAG_APPMODE);

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        if (appModeTag != null) {

            switch (appModeTag) {

                case AppModes.VPLAN:
                    //update appmode
                    pref.edit().putInt(SharedPrefs.APPMODE, AppModes.VPLAN).apply();

                    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    break;
                case AppModes.TESTS:
                    //update appmode
                    pref.edit().putInt(SharedPrefs.APPMODE, AppModes.TESTS).apply();

                    startActivity(new Intent(this, ExamsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }
    }

    public void onSettingsClick(View view) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        int day = pager.getCurrentItem();
        DataSource datasource = new DataSource(this);

        //a button in the add-lesson dialog was clicked, determine which one
        switch (which) {

            case DialogInterface.BUTTON_POSITIVE:
                //save the entered data
                AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.subjectACTV);
                AutoCompleteTextView roomACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.roomACTV);
                MyNumberPicker lessonPicker = (MyNumberPicker) dialogView.findViewById(R.id.lessonPicker);

                addLesson(datasource, day, lessonPicker.getValue(), subjectACTV.getText().toString(), roomACTV.getText().toString());

                saveSubject(datasource, subjectACTV.getText().toString());

                //refresh the timetable views
                TimetablePagerAdapter adapter = new TimetablePagerAdapter(this, getSupportFragmentManager());
                pager.setAdapter(adapter);
                pager.setCurrentItem(day);
                break;
            default:
                dialog.dismiss();
                break;
        }
    }

    private void saveSubject(DataSource datasource, String subject) {

        //add this subject to the database if it doesn't exist already
        datasource.open();
        Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_SUBJECTS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_SUBJECT}, SQLiteHelperTimetable.COLUMN_SUBJECT + "='" + subject + "'");

        if (c.getCount() == 0) datasource.addSubject(subject);

        datasource.close();
    }

    @Override
    public boolean onLongClick(final View v) {

        //show a dialog and ask the user if this lesson should be edited or deleted
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.edit_or_delete))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView lesson = (TextView) v.findViewById(R.id.lesson);
                        removeLesson(new DataSource(TimetableActivity.this), lesson.getText().toString());
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editLesson(v);
                    }
                }).show();

        return true;
    }

    private void editLesson(View lessonView) {

        //show the add lesson dialog but with the values of the lesson being edited preset
        dialogView = View.inflate(this, R.layout.add_lesson_dialog, null);

        //load the values
        AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.subjectACTV);
        TextView subjectOld = (TextView) lessonView.findViewById(R.id.subject);
        subjectACTV.setText(subjectOld.getText());

        AutoCompleteTextView roomACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.roomACTV);
        TextView roomOld = (TextView) lessonView.findViewById(R.id.room);
        roomACTV.setText(roomOld.getText());

        MyNumberPicker lessonPicker = (MyNumberPicker) dialogView.findViewById(R.id.lessonPicker);
        TextView lesson = (TextView) lessonView.findViewById(R.id.lesson);
        lessonPicker.setValue(Integer.parseInt(lesson.getText().toString()));

        //save the values in editingRow
        editingRow = new TimetableRow(lesson.getText().toString(), subjectOld.getText().toString(), roomOld.getText().toString());

        //now init the dialog and show it
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_lesson))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveEditedLesson(new DataSource(TimetableActivity.this));
                    }
                })
                .setNegativeButton(R.string.cancel, this)
                .show();
    }

    private void saveEditedLesson(DataSource datasource) {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        int day = pager.getCurrentItem();

        //save the entered data
        AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.subjectACTV);
        AutoCompleteTextView roomACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.roomACTV);
        MyNumberPicker lessonPicker = (MyNumberPicker) dialogView.findViewById(R.id.lessonPicker);
        //TODO die actv's aktivieren

        //db insert
        datasource.open();

        datasource.updateRowTimetable(SQLiteHelperTimetable.DAYS[day], String.valueOf(lessonPicker.getValue()), subjectACTV.getText().toString(), roomACTV.getText().toString(), editingRow);

        datasource.close();

        saveSubject(datasource, subjectACTV.getText().toString());

        //refresh the timetable views
        TimetablePagerAdapter adapter = new TimetablePagerAdapter(this, getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setCurrentItem(day);
    }

    private void removeLesson(DataSource datasource, String lesson) {

        //find out the currently visible day
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        int position = pager.getCurrentItem();
        String tableName = SQLiteHelperTimetable.DAYS[position];

        //delete the row in this day's timetable that contains this lesson attribute
        datasource.open();
        datasource.deleteRowTimetable(tableName, lesson);
        datasource.close();

        TimetablePagerAdapter adapter = new TimetablePagerAdapter(this, getSupportFragmentManager());
        pager.setAdapter(adapter);
    }

    private class PagerAdapterLoader extends AsyncTask<Void, Void, Void> {

        TimetablePagerAdapter adapter;

        @Override
        protected Void doInBackground(Void... params) {

            //load the pager adapter in the background
            adapter = new TimetablePagerAdapter(TimetableActivity.this, TimetableActivity.this.getSupportFragmentManager());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
            progress.setVisibility(View.GONE);
            PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
            tabStrip.setVisibility(View.VISIBLE);

            initPager(adapter);
        }
    }
}
