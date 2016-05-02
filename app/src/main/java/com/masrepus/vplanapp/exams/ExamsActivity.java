package com.masrepus.vplanapp.exams;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.ProgressCode;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperTests;
import com.masrepus.vplanapp.network.AsyncDownloader;
import com.masrepus.vplanapp.settings.SettingsActivity;
import com.masrepus.vplanapp.settings.SettingsPrefListener;
import com.masrepus.vplanapp.timetable.TimetableActivity;
import com.masrepus.vplanapp.vplan.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class ExamsActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    public static final String ACTIVITY_NAME = "exams";
    private static final int REQUEST_CALENDAR = 0;
    private ArrayList<ExamsRow> examsList;
    private MenuItem refreshItem;
    private boolean noOldItems;
    private SettingsPrefListener listener;
    private String callingActivity;

    private ShowcaseView showcase;
    private boolean tutorialMode;
    private ArrayList<CalendarItem> calIds;
    private AlertDialog reminderDialog;
    private int timespanFactor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exams);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setSubtitle(getString(R.string.exams_activity_subtitle));

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        noOldItems = pref.getBoolean(SharedPrefs.HIDE_OLD_EXAMS, false);
        tutorialMode = !pref.getBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "ExamsActivity", false);

        //hide or show hidden items info
        FrameLayout hiddenItemsFL = (FrameLayout) findViewById(R.id.frameLayout2);
        if (noOldItems) hiddenItemsFL.setVisibility(View.VISIBLE);
        else hiddenItemsFL.setVisibility(View.GONE);

        //init the drawer
        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.setNavigationItemSelectedListener(this);
        drawer.getMenu().findItem(R.id.exams_appmode_item).setChecked(true);

        //display last update timestamp
        String lastUpdate = getString(R.string.last_update) + " " + pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + AppModes.TESTS, "");
        drawer.getMenu().findItem(R.id.lastUpdate).setTitle(lastUpdate);

        refreshAdapter();

        if (tutorialMode) {
            showTutorial();
            pref.edit().putBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "ExamsActivity", true).apply();
        }
    }

    private void showTutorial() {
        showcase = new ShowcaseView.Builder(this)
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .setContentTitle(getString(R.string.title_activity_exams))
                .setContentText(getString(R.string.tut_exams))
                .build();
        showcase.setHideOnTouchOutside(false);
        showcase.setButtonPosition(getRightParam(getResources()));
        showcase.setBlocksTouches(false);
        showcase.show();
    }

    public RelativeLayout.LayoutParams getRightParam(Resources res) {
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = ((Number) (res.getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, getNavigationBarHeight(res.getConfiguration().orientation
        ) + (int) res.getDisplayMetrics().density * 10);
        return lps;
    }

    public int getNavigationBarHeight(int orientation) {
        try {
            Resources resources = getResources();
            int id = resources.getIdentifier(
                    orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape",
                    "dimen", "android");
            if (id > 0) {
                return resources.getDimensionPixelSize(id);
            }
        } catch (NullPointerException | IllegalArgumentException | Resources.NotFoundException e) {
            return 0;
        }
        return 0;
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

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        //set the appmode to exams
        editor.putInt(SharedPrefs.APPMODE, AppModes.TESTS);
        editor.apply();

        //refresh the drawer
        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.getMenu().findItem(R.id.vplan_appmode_item).setChecked(false);
        drawer.getMenu().findItem(R.id.exams_appmode_item).setChecked(true);
        drawer.getMenu().findItem(R.id.timetable_appmode_item).setChecked(false);
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
        ExamsListAdapter adapter = new ExamsListAdapter(this, R.layout.list_item_exam, examsList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.exams, menu);

        //init the filter item
        MenuItem filterItem = menu.findItem(R.id.action_activate_filter);
        filterItem.setChecked(noOldItems);

        tintIcons(menu);

        return true;
    }

    private void tintIcons(Menu menu) {

        //get the tint color
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.tintMenu, typedValue, true);
        int color = typedValue.data;

        //tint the items according to our theme
        MenuItem item = menu.findItem(R.id.action_refresh);
        Drawable newIcon = item.getIcon();
        newIcon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        item.setIcon(newIcon);

        item = menu.findItem(R.id.action_help);
        newIcon = item.getIcon();
        newIcon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        item.setIcon(newIcon);
    }

    @Override
    protected void onPause() {

        //save the filter state
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(SharedPrefs.HIDE_OLD_EXAMS, noOldItems)
                .putInt(SharedPrefs.APPMODE, AppModes.VPLAN);
        editor.apply();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);

        super.onPause();
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
                                        Uri uri = Uri.parse(AsyncDownloader.findRequestedTestsPage(getApplicationContext(), VplanModes.MINFO));
                                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                        break;
                                    case 1:
                                        //oinfo
                                        uri = Uri.parse(AsyncDownloader.findRequestedTestsPage(getApplicationContext(), VplanModes.OINFO));
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
            case R.id.action_help:
                pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                pref.edit().putBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "ExamsActivity", true).apply();
                tutorialMode = true;
                showTutorial();
                return true;
            case R.id.action_add_to_calendar:
                checkCalendarPermission();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkCalendarPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

            //should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CALENDAR) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {

                final Activity activity = this;

                //explain to the user that we need this permission in order to add calendar events
                new AlertDialog.Builder(this)
                        .setMessage(R.string.perm_rationale_calendar)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, REQUEST_CALENDAR);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return;
            }

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, REQUEST_CALENDAR);
        } else selectCalendar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case REQUEST_CALENDAR:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //permission granted! now continue with inserting the exams into the calendar
                    selectCalendar();
                } else {

                    //permission denied; tell the user that he can't continue like that
                    Toast.makeText(this, R.string.perm_rationale_calendar, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void addToCalendar(CalendarItem calendar, int minutes) {

        TimeZone tz = TimeZone.getDefault();
        ArrayList<Exam> exams = getExams();
        int saved = 0;

        //now insert each exam to the calendar
        for (Exam exam : exams) {

            //calendar insert
            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, exam.getTimeInMillis());
            values.put(CalendarContract.Events.DTEND, exam.getTimeInMillis());
            values.put(CalendarContract.Events.TITLE, exam.getTitle());
            values.put(CalendarContract.Events.CALENDAR_ID, calendar.getId());
            values.put(CalendarContract.Events.ALL_DAY, 1);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getID());
            if (minutes > 0) values.put(CalendarContract.Events.HAS_ALARM, 1);
            else values.put(CalendarContract.Events.HAS_ALARM, 0);

            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

            //log the event id
            if (uri != null) {
                Long id = new Long(uri.getLastPathSegment());
                Log.d(ACTIVITY_NAME, "Exam " + exam.getTitle() + " added to calendar " + calendar.getName() + " with ID " + id);

                //now add a reminder if the user requested this
                if (minutes > 0) {
                    values.clear();
                    values.put(CalendarContract.Reminders.EVENT_ID, id);
                    values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                    values.put(CalendarContract.Reminders.MINUTES, minutes);
                    cr.insert(CalendarContract.Reminders.CONTENT_URI, values);

                    Log.d(ACTIVITY_NAME, "Reminder added to exam with ID " + id + " for " + minutes + " minutes before start");
                }
                saved++;
            }
        }

        //check success and notify the user
        if (saved == exams.size()) Toast.makeText(this, getString(R.string.cal_insert_success), Toast.LENGTH_LONG).show();
        else if (saved < exams.size()) Toast.makeText(this, (exams.size() - saved) + getString(R.string.cal_insert_error), Toast.LENGTH_LONG).show();
    }

    private ArrayList<Exam> getExams() {

        DataSource datasource = new DataSource(this);
        datasource.open();

        ArrayList<Exam> exams = new ArrayList<>();

        //get the data from both test tables
        Cursor c = datasource.query(SQLiteHelperTests.TABLE_TESTS_UINFO_MINFO, new String[]{SQLiteHelperTests.COLUMN_DATE, SQLiteHelperTests.COLUMN_GRADE, SQLiteHelperTests.COLUMN_TYPE, SQLiteHelperTests.COLUMN_SUBJECT});

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        String dateString, grade, type, subject;
        Date date;

        //parse the dates and add them to the exams list
        while (c.moveToNext()) {

            try {
                dateString = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_DATE));
                grade = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_GRADE));
                type = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_TYPE));
                subject = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_SUBJECT));

                date = format.parse(dateString);

                //grade + type + subject goes to the title
                exams.add(new Exam(date.getTime(), grade + ": " + type + " in " + subject));
            } catch (ParseException ignored) {
            }
        }

        //now oinfo
        c = datasource.query(SQLiteHelperTests.TABLE_TESTS_OINFO, new String[]{SQLiteHelperTests.COLUMN_DATE, SQLiteHelperTests.COLUMN_GRADE, SQLiteHelperTests.COLUMN_TYPE, SQLiteHelperTests.COLUMN_SUBJECT});

        while (c.moveToNext()) {

            try {
                dateString = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_DATE));
                grade = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_GRADE));
                type = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_TYPE));
                subject = c.getString(c.getColumnIndex(SQLiteHelperTests.COLUMN_SUBJECT));

                date = format.parse(dateString);

                //grade + type + subject goes to the title
                exams.add(new Exam(date.getTime(), grade + ": " + type + " in " + subject));
            } catch (ParseException ignored) {
            }
        }

        datasource.close();

        return exams;
    }

    private void selectCalendar() {

        //query for all available calendars
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        };

        Cursor calendarCursor = managedQuery(uri, projection, null, null, null);
        calIds = new ArrayList<>();

        //save the calendar ids to an arraylist
        while (calendarCursor.moveToNext()) {

            int id = calendarCursor.getInt(calendarCursor.getColumnIndex(CalendarContract.Calendars._ID));
            String name = calendarCursor.getString(calendarCursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
            calIds.add(new CalendarItem(id, name));
        }

        calendarCursor.moveToFirst();

        //ask the user to choose the calendar into which the exams should be saved
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_calendar_title)
                .setCursor(calendarCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        showReminderDialog(calIds.get(position));
                    }
                }, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        builder.show();
    }

    private void showReminderDialog(final CalendarItem calendar) {

        //show a dialog where the user can choose to set a default reminder time for exams
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_add_reminder)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText timeET = (EditText) reminderDialog.findViewById(R.id.timeEditText);
                        int time = Integer.parseInt(timeET.getText().toString());

                        //multiply the contents of the edit text by timespanFactor
                        time = time * timespanFactor;
                        addToCalendar(calendar, time);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addToCalendar(calendar, -1);
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.cancel, null);
        reminderDialog = builder.create();
        reminderDialog.setView(View.inflate(this, R.layout.dialog_add_calendar_reminder, null));

        reminderDialog.show();

        //populate the spinner
        Spinner timespanSpinner = (Spinner) reminderDialog.findViewById(R.id.timespanSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_simple, R.id.itemTV, new String[]{getString(R.string.min_before), getString(R.string.hours_before), getString(R.string.days_before)});
        timespanSpinner.setAdapter(adapter);
        timespanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        timespanFactor = 1;
                        break;
                    case 1:
                        timespanFactor = 60;
                        break;
                    case 2:
                        timespanFactor = 1440; //1440 minutes in a day
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                timespanFactor = 1;
            }
        });
    }

    private ArrayList<ExamsRow> initData() {

        DataSource datasource = new DataSource(this);
        datasource.open();

        ArrayList<ExamsRow> examsList = new ArrayList<>();

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
        ImageView iv = (ImageView) inflater.inflate(R.layout.view_action_refresh, null);

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

    @Override
    public void onClick(View view) {
    }

    public void displayLastUpdate(String lastUpdate) {

        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.getMenu().findItem(R.id.lastUpdate).setTitle(lastUpdate);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        Menu menu = drawer.getMenu();

        switch (menuItem.getItemId()) {

            case R.id.vplan_appmode_item:
                //update appmode
                pref.edit().putInt(SharedPrefs.APPMODE, AppModes.VPLAN).apply();
                menu.findItem(R.id.exams_appmode_item).setChecked(false);
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case R.id.timetable_appmode_item:
                //update appmode
                pref.edit().putInt(SharedPrefs.APPMODE, AppModes.TIMETABLE).apply();
                menu.findItem(R.id.exams_appmode_item).setChecked(false);
                startActivity(new Intent(this, TimetableActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case R.id.settings_item:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                break;
        }

        return true;
    }

    private class CalendarItem {

        private int id;
        private String name;

        public CalendarItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
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
                    convertView = View.inflate(context, R.layout.list_item_exam, null);
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
                    for (int i = 0; i < 2; i++) {
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
            return AppModes.TESTS;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            progressBar.setVisibility(View.GONE);

            resetRefreshAnimation();

            refreshAdapter();

            displayLastUpdate(refreshLastUpdate());
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
                    progressBar.setProgress((int) (100 * (double) downloaded_files / total_downloads));
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

    private class Exam {

        private long timeInMillis;
        private String title;

        public Exam(long timeInMillis, String title) {
            this.timeInMillis = timeInMillis;
            this.title = title;
        }

        public long getTimeInMillis() {
            return timeInMillis;
        }

        public String getTitle() {
            return title;
        }
    }
}
