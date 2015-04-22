package com.masrepus.vplanapp.timetable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.exams.ExamsActivity;
import com.masrepus.vplanapp.settings.SettingsActivity;
import com.masrepus.vplanapp.settings.SettingsPrefListener;
import com.masrepus.vplanapp.vplan.MainActivity;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperTimetable;
import com.masrepus.vplanapp.drawer.DrawerListAdapter;
import com.masrepus.vplanapp.drawer.DrawerListBuilder;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class TimetableActivity extends ActionBarActivity implements View.OnClickListener, DialogInterface.OnClickListener, View.OnLongClickListener, Serializable {

    private SimpleDateFormat weekdays = new SimpleDateFormat("EEEE");
    private TimetableRow editingRow;
    private SettingsPrefListener listener;
    private String callingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timetable_activity);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //activate the fab
        ImageButton fab = (ImageButton) findViewById(R.id.fab_image_button);
        fab.setOnClickListener(this);

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

        //check if this activity has to take care of shared prefs changes
        callingActivity = getIntent().getStringExtra(Args.CALLING_ACTIVITY);
        if (callingActivity == null) callingActivity = "";

        if (!callingActivity.contentEquals("")) {
            listener = new SettingsPrefListener(this);
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
        }

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

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);

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
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAddLessonDialog(int lessonPreset) {

        //now show the user a dialog where he can add a new lesson
        View dialogView = View.inflate(this, R.layout.add_lesson_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_lesson))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this);
        AlertDialog dialog = builder.create();
        dialog.show();

        //put the dialog to the complete top of the screen so that the actv dropdowns can be seen better
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);

        //init the actv adapters
        DataSource datasource = new DataSource(this);
        datasource.open();

        Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_SUBJECTS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_SUBJECT});
        ArrayList<String> subjects = new ArrayList<>();

        while (c.moveToNext()) {
            subjects.add(c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_SUBJECT)));
        }

        c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_ROOMS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_ROOM});
        ArrayList<String> rooms = new ArrayList<>();

        while (c.moveToNext()) {
            rooms.add(c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_ROOM)));
        }

        datasource.close();


        AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.subjectACTV);
        subjectACTV.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects));

        AutoCompleteTextView roomsACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.roomACTV);
        roomsACTV.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rooms));

        MyNumberPicker lessonPicker = (MyNumberPicker) dialogView.findViewById(R.id.lessonPicker);
        if (lessonPreset != 0) {

            //preset the selected lesson in the dialog
            lessonPicker.setLesson(lessonPreset);
        } else {

            //set the lesson to the last saved lesson + 1
            SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            int day = pager.getCurrentItem();
            int lesson = pref.getInt(SharedPrefs.MAX_LESSON + day, 1) + 1;
            lessonPicker.setLesson(lesson);
        }
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

        //check if this is the fab
        if (view.getId() == R.id.fab_image_button) {

            //add a lesson to the currently visible day
            showAddLessonDialog(0);
        } else {

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
    }

    public void onSettingsClick(View view) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        int day = pager.getCurrentItem();
        DataSource datasource = new DataSource(this);

        AlertDialog alertDialog = (AlertDialog) dialog;

        //a button in the add-lesson dialog was clicked, determine which one
        switch (which) {

            case DialogInterface.BUTTON_POSITIVE:
                //save the entered data
                AutoCompleteTextView subjectACTV = (AutoCompleteTextView) alertDialog.findViewById(R.id.subjectACTV);
                AutoCompleteTextView roomACTV = (AutoCompleteTextView) alertDialog.findViewById(R.id.roomACTV);
                MyNumberPicker lessonPicker = (MyNumberPicker) alertDialog.findViewById(R.id.lessonPicker);

                addLesson(datasource, day, lessonPicker.getLesson(), subjectACTV.getText().toString(), roomACTV.getText().toString());

                saveSubject(datasource, subjectACTV.getText().toString());
                saveRoom(datasource, roomACTV.getText().toString());

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

    private void saveRoom(DataSource datasource, String room) {

        //add this room to the database if it doesn't exist already
        datasource.open();
        Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_ROOMS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_ROOM}, SQLiteHelperTimetable.COLUMN_ROOM + "='" + room + "'");

        if (c.getCount() == 0) datasource.addRoom(room);

        datasource.close();
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

        //check if this is a free lesson
        TextView subject = (TextView) v.findViewById(R.id.subject);
        if (subject.getText().toString().contentEquals(getString(R.string.free_lesson))) {

            TextView lesson = (TextView) v.findViewById(R.id.lesson);

            //immediately show the add lesson dialog with this row's lesson pre-set
            showAddLessonDialog(Integer.valueOf(lesson.getText().toString()));
        } else {

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
        }

        return true;
    }

    private void editLesson(View lessonView) {

        //show the add lesson dialog but with the values of the lesson being edited preset
        View dialogView = View.inflate(this, R.layout.add_lesson_dialog, null);

        //load the values
        AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.subjectACTV);
        final TextView subjectOld = (TextView) lessonView.findViewById(R.id.subject);
        subjectACTV.setText(subjectOld.getText());

        AutoCompleteTextView roomACTV = (AutoCompleteTextView) dialogView.findViewById(R.id.roomACTV);
        final TextView roomOld = (TextView) lessonView.findViewById(R.id.room);
        roomACTV.setText(roomOld.getText());

        MyNumberPicker lessonPicker = (MyNumberPicker) dialogView.findViewById(R.id.lessonPicker);
        final TextView lesson = (TextView) lessonView.findViewById(R.id.lesson);
        lessonPicker.setLesson(Integer.parseInt(lesson.getText().toString()));

        //save the values in editingRow
        editingRow = new TimetableRow(lesson.getText().toString(), subjectOld.getText().toString(), roomOld.getText().toString());

        //init the actv adapters
        DataSource datasource = new DataSource(this);
        datasource.open();

        Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_SUBJECTS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_SUBJECT});
        ArrayList<String> subjects = new ArrayList<>();

        while (c.moveToNext()) {
            subjects.add(c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_SUBJECT)));
        }

        c = datasource.queryTimetable(SQLiteHelperTimetable.TABLE_ROOMS_ACTV, new String[]{SQLiteHelperTimetable.COLUMN_ROOM});
        ArrayList<String> rooms = new ArrayList<>();

        while (c.moveToNext()) {
            rooms.add(c.getString(c.getColumnIndex(SQLiteHelperTimetable.COLUMN_ROOM)));
        }

        datasource.close();


        subjectACTV.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects));
        roomACTV.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rooms));

        //now init the dialog and show it
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_lesson))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveEditedLesson((AlertDialog) dialog, new DataSource(TimetableActivity.this), new TimetableRow(lesson.getText().toString(), subjectOld.getText().toString(), roomOld.getText().toString()));
                    }
                })
                .setNegativeButton(R.string.cancel, this);

        AlertDialog dialog = builder.create();
        dialog.show();

        //put the dialog to the complete top of the screen so that the actv dropdowns can be seen better
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
    }

    private void saveEditedLesson(AlertDialog dialog, DataSource datasource, TimetableRow oldData) {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        int day = pager.getCurrentItem();

        //save the entered data
        AutoCompleteTextView subjectACTV = (AutoCompleteTextView) dialog.findViewById(R.id.subjectACTV);
        AutoCompleteTextView roomACTV = (AutoCompleteTextView) dialog.findViewById(R.id.roomACTV);
        MyNumberPicker lessonPicker = (MyNumberPicker) dialog.findViewById(R.id.lessonPicker);

        //db insert
        datasource.open();

        Cursor c = datasource.queryTimetable(SQLiteHelperTimetable.DAYS[day], new String[]{SQLiteHelperTimetable.COLUMN_LESSON}, SQLiteHelperTimetable.COLUMN_LESSON + "='" + lessonPicker.getLesson() + "'");

        //check if there is an entry in that lesson
        if (c.getCount() == 0) {
            datasource.updateRowTimetable(SQLiteHelperTimetable.DAYS[day], String.valueOf(lessonPicker.getLesson()), subjectACTV.getText().toString(), roomACTV.getText().toString(), editingRow);

            saveSubject(datasource, subjectACTV.getText().toString());
            saveRoom(datasource, roomACTV.getText().toString());
        } else {

            //first delete the data that was in that lesson
            datasource.deleteRowTimetable(SQLiteHelperTimetable.DAYS[day], String.valueOf(lessonPicker.getLesson()));

            //now delete this row's old data
            datasource.deleteRowTimetable(SQLiteHelperTimetable.DAYS[day], oldData.getLesson());

            //now save the new data
            datasource.createRowTimetable(SQLiteHelperTimetable.DAYS[day], String.valueOf(lessonPicker.getLesson()), subjectACTV.getText().toString(), roomACTV.getText().toString());

        }

        //refresh the timetable views
        TimetablePagerAdapter adapter = new TimetablePagerAdapter(this, getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setCurrentItem(day);

        datasource.close();
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
            initPager(adapter);
        }
    }
}
