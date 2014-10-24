package com.masrepus.vplanapp;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

public class MainActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFS_NAME = "mPrefs";
    public static final String PREF_LAST_UPDATE = "lastUpdate";
    public static final String PREF_VPLAN_MODE = "mode";
    public static final String PREF_PREFIX_VPLAN_CURR_DATE = "currDate";
    public static final String PREF_PREFIX_VPLAN_TIME_PUBLISHED = "timePublished";
    public static final String PREF_IS_FILTER_ACTIVE = "isFilterActive";
    public static final String PREF_APPMODE = "appmode";
    public static final String PREF_TODAY_VPLAN = "todayVplan";
    public static final String PREF_IS_BG_UPD_ACTIVE = "isBgUpdActive";
    public static final String PREF_CURR_BG_INT = "currInt";
    public static final String PREF_REQUESTED_VPLAN_ID = "requestedVplanId";
    public static final String PREF_CURR_VPLAN_LINK = "currVplanLink";
    public static final int BASIC = 0;
    public static final int UINFO = 1;
    public static final int MINFO = 2;
    public static final int OINFO = 3;
    public static final java.text.DateFormat standardFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
    public static final int VPLAN = 0;
    private int appMode = VPLAN; //at the moment tests is not available
    public static final int TESTS = 1;
    public static int inflateStatus = 0;
    public ArrayList<String> filterCurrent = new ArrayList<String>();
    public ArrayList<String> filterUnterstufe = new ArrayList<String>();
    public ArrayList<String> filterMittelstufe = new ArrayList<String>();
    public ArrayList<String> filterOberstufe = new ArrayList<String>();
    private int requestedVplanMode;
    private int requestedVplanId;
    private boolean isOnlineRequested;
    private VPlanDataSource datasource;
    private MenuItem refreshItem;
    private ActionBarDrawerToggle drawerToggle;
    private String timePublished;
    private Map<String, ?> keys;
    private String currentVPlanLink;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        datasource = new VPlanDataSource(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView welcome = (TextView) findViewById(R.id.welcome_textView);
        welcome.setVisibility(View.VISIBLE);

        //get the state of the filter from shared prefs
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        pref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                requestedVplanId = sharedPreferences.getInt(PREF_REQUESTED_VPLAN_ID, 0);
                currentVPlanLink = sharedPreferences.getString(PREF_CURR_VPLAN_LINK, "");
                timePublished = sharedPreferences.getString(PREF_PREFIX_VPLAN_TIME_PUBLISHED, "");
            }
        });
        requestedVplanMode = pref.getInt(PREF_VPLAN_MODE, UINFO);
        appMode = pref.getInt(PREF_APPMODE, VPLAN);
        if (pref.getBoolean(PREF_IS_FILTER_ACTIVE, false)) {
            FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);
            fl.setVisibility(View.VISIBLE);
            TextView filterWarning = (TextView) findViewById(R.id.filterWarning);
            filterWarning.setText(R.string.filter_enabled);
        }

        //display current app info in appinfo textview
        TextView appInfo = (TextView) findViewById(R.id.textViewAppInfo);
        try {
            appInfo.setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " by Samuel Hopstock");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appInfo.setText("Fehler");
        }

        refreshFilters();

        //activate adapter for viewPager
        VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), this, this, filterCurrent);

        //if the adapter's number of fragments is 0, then display the no data layout on top of it

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(vplanPagerAdapter);

        //set a 1 dp margin between the fragments, filled with the divider_vertical drawable
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabStrip.setTabIndicatorColor(getResources().getColor(R.color.blue));

        //initialise navigation drawer
        TextView settings = (TextView) findViewById(R.id.textViewSettings);
        settings.setText(getString(R.string.settings).toUpperCase());

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        final Context context = this;
        if (actionBar != null) {

            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_closed) {

                private int currMode;

                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);

                    //set title corresponding to requested vplan mode
                    switch (requestedVplanMode) {

                        case UINFO:
                            actionBar.setTitle(R.string.unterstufe);
                            break;
                        case MINFO:
                            actionBar.setTitle(R.string.mittelstufe);
                            break;
                        case OINFO:
                            actionBar.setTitle(R.string.oberstufe);
                            break;
                    }

                    if (currMode != requestedVplanMode) {

                        welcome.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    actionBar.setTitle(R.string.sgp);

                    //save the content of requestedVplanMode for later check for change
                    currMode = requestedVplanMode;
                }
            };

            drawerLayout.setDrawerListener(drawerToggle);

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            //set the actionbar title matching to requestedVplanMode
            switch (requestedVplanMode) {

                case UINFO:
                    actionBar.setTitle(R.string.unterstufe);
                    break;
                case MINFO:
                    actionBar.setTitle(R.string.mittelstufe);
                    break;
                case OINFO:
                    actionBar.setTitle(R.string.oberstufe);
                    break;
            }
        }

        //display last update timestamp
        String lastUpdate = pref.getString(PREF_LAST_UPDATE, "");
        TextView tv = (TextView) findViewById(R.id.lastUpdate);
        tv.setVisibility(View.VISIBLE);
        tv.setText(lastUpdate);

        //restore the last checked radio button
        RadioButton currModeRadio = null;
        switch (requestedVplanMode) {

            case UINFO:
                currModeRadio = (RadioButton) findViewById(R.id.radioUinfo);
                break;
            case MINFO:
                currModeRadio = (RadioButton) findViewById(R.id.radioMinfo);
                break;
            case OINFO:
                currModeRadio = (RadioButton) findViewById(R.id.radioOinfo);
                break;
        }

        //check whether refresh is necessary
        if (currModeRadio != null) {
            onVplanModeRadioButtonClick(currModeRadio);
            currModeRadio.setChecked(true);
        }

        //restore the last checked appmode radiobutton
        RadioButton currAppmodeRadio = null;
        switch (appMode) {

            case VPLAN:
                currAppmodeRadio = (RadioButton) findViewById(R.id.radioVPlan);
                break;
            case TESTS:
                currAppmodeRadio = (RadioButton) findViewById(R.id.radioTests);
                break; //commented out for public release
        }

        //check whether refresh is necessary
        if (currAppmodeRadio != null) {
            onModeChangeRadioButtonClick(currAppmodeRadio);
            currAppmodeRadio.setChecked(true);
        }

        //register change listener for settings sharedPrefs
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settingsPrefs.registerOnSharedPreferenceChangeListener(this);

    }

    private int getTodayVplanId() {

        //today's vplan id has been saved in sharedprefs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_TODAY_VPLAN, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //refresh the pager adapter
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setCurrentItem(getTodayVplanId());
    }

    /**
     * Called when one of the RadioButtons in the navigation drawer is clicked
     */
    public void onVplanModeRadioButtonClick(View v) {

        //update the requested vplanmode and check whether a different one has been selected
        switch (v.getId()) {

            case R.id.radioMinfo:

                rbUiHandler(new int[]{R.id.radioMinfo, R.id.radioOinfo, R.id.radioUinfo}, new int[]{R.id.radioMinfoFrame, R.id.radioOinfoFrame, R.id.radioUinfoFrame});

                requestedVplanMode = MINFO;

                //update current filterlist
                filterCurrent = filterMittelstufe;
                break;

            case R.id.radioOinfo:

                rbUiHandler(new int[]{R.id.radioOinfo, R.id.radioMinfo, R.id.radioUinfo}, new int[]{R.id.radioOinfoFrame, R.id.radioMinfoFrame, R.id.radioUinfoFrame});

                requestedVplanMode = OINFO;

                //update current filterlist
                filterCurrent = filterOberstufe;
                break;

            case R.id.radioUinfo:

                rbUiHandler(new int[]{R.id.radioUinfo, R.id.radioOinfo, R.id.radioMinfo}, new int[]{R.id.radioUinfoFrame, R.id.radioOinfoFrame, R.id.radioMinfoFrame});

                requestedVplanMode = UINFO;

                //update current filterlist
                filterCurrent = filterUnterstufe;

                break;
        }

        //save mode in shared prefs
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PREF_VPLAN_MODE, requestedVplanMode);
        editor.apply();

        //refresh adapter for viewPager
        VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), this, this, filterCurrent);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(vplanPagerAdapter);

        //set a 1 dp margin between the fragments
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
    }

    public void onModeChangeRadioButtonClick(View v) {

        switch (v.getId()) {

            case R.id.radioTests:
                rbUiHandler(new int[]{R.id.radioTests, R.id.radioVPlan}, new int[]{R.id.radioTestsFrame, R.id.radioVPlanFrame});

                //nothing
                break;
            case R.id.radioVPlan:
                rbUiHandler(new int[]{R.id.radioVPlan, R.id.radioTests}, new int[]{R.id.radioVPlanFrame, R.id.radioTestsFrame});

                break;
        }
    }

    private void rbUiHandler(int[] buttons, int[] frames) {

        //add all radiobuttons associated to the given id's to an arraylist and then handle the layout changes automatically
        ArrayList<RadioButton> radioButtons = new ArrayList<RadioButton>(buttons.length);

        for (int id : buttons) {
            radioButtons.add((RadioButton) findViewById(id));
        }

        radioButtons.get(0).setTypeface(Typeface.DEFAULT_BOLD);

        for (int i = 1; i <= radioButtons.size() - 1; i++) {
            radioButtons.get(i).setTypeface(Typeface.DEFAULT);
        }

        //same thing for the framelayouts
        ArrayList<FrameLayout> frameLayouts = new ArrayList<FrameLayout>(frames.length);

        for (int id : frames) {
            frameLayouts.add((FrameLayout) findViewById(id));
        }

        frameLayouts.get(0).setEnabled(true);

        for (int i = 1; i <= frameLayouts.size() - 1; i++) {
            frameLayouts.get(i).setEnabled(false);
        }
    }

    /**
     * Called when the settings textview in the drawer is clicked
     */
    public void onSettingsClick(View v) {
        //open settings
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    /**
     * Called when SettingsActivity returns a result, which is when it is destroyed
     *
     * @param requestCode is always 0
     * @param resultCode  is always 0 as well
     * @param data        no data is being sent by SettingsActivity, so always null
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new VplanPagerAdapter(getSupportFragmentManager(), this, this, filterCurrent));
    }

    /**
     * Called after everything is initialised and syncs the navigation drawer toggle in the actionbar
     */
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setCurrentItem(getTodayVplanId());
    }

    /**
     * Called by findRequestedVplan in order to find the correct vplan url
     *
     * @param version            either UINFO, MINFO or OINFO, depending on the current mode
     * @param includeCredentials only true if the url will be used to open the website in a browser, which appends username:password@ before the url
     * @return the url as a String
     */
    private String getVPlanUrl(int version, boolean includeCredentials) {

        // depending on the version requested return the appropriate string, optionally with credentials inside header
        String vplanBase;

        if (includeCredentials) {

            //uname and pwd are stored in settings sharedprefs
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String uname = pref.getString(getString(R.string.key_uname), "");
            String pwd = pref.getString(getString(R.string.key_pwd), "");

            vplanBase = "http://" + uname + ":" + pwd + "@" + getString(R.string.vplan_base_cred);
        } else {
            vplanBase = getString(R.string.vplan_base_url);
        }

        String url = "";
        switch (version) {
            case BASIC:
                return vplanBase;
            case UINFO:
                url = vplanBase + "pw/" + "urekursiv.php";
                break;
            case MINFO:
                url = vplanBase + "pw/" + "mrekursiv.php";
                break;
            case OINFO:
                url = vplanBase + "oinfo/" + "srekursiv.php";
                break;
        }

        if (currentVPlanLink != null && !currentVPlanLink.contentEquals("")) {
            String date = currentVPlanLink.split("_")[2];

            return url + "?datei=schuelerplan_vom_" + date;
        } else return url;
    }

    /**
     * Prepares the actionbar menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_actions, menu);
        refreshItem = menu.findItem(R.id.action_refresh);

        //check whether the filterItem should be checked or not
        MenuItem filterItem = menu.findItem(R.id.action_activate_filter);
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        if (pref.getBoolean(PREF_IS_FILTER_ACTIVE, false)) {
            filterItem.setChecked(true);
        } else filterItem.setChecked(false);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Called when an actionbar item is clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //pass the event to drawerToggle, if it returns true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh(item);
                return true;
            case R.id.action_open_browser:
                //fire an action_view intent with the vplan url that contains creds
                Uri link = Uri.parse(getVPlanUrl(requestedVplanMode, true));
                Intent intent = new Intent(Intent.ACTION_VIEW, link);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            case R.id.action_activate_filter:
                //display the grey status bar for the filter or disable it
                FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);
                TextView filterWarning = (TextView) findViewById(R.id.filterWarning);
                if (item.isChecked()) {
                    item.setChecked(false);
                    SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean(PREF_IS_FILTER_ACTIVE, false);
                    editor.apply();
                    fl.setVisibility(View.GONE);
                } else {
                    item.setChecked(true);
                    SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = pref.edit();

                    editor.putBoolean(PREF_IS_FILTER_ACTIVE, true);
                    editor.apply();
                    fl.setVisibility(View.VISIBLE);
                    filterWarning.setText(R.string.filter_enabled);
                }
                //refresh adapter for viewPager
                VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), this, this, filterCurrent);
                ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
                viewPager.setAdapter(vplanPagerAdapter);
                viewPager.setCurrentItem(getTodayVplanId(), false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refresh(View v) {
        refresh(refreshItem);
    }

    /**
     * Called when the refresh actionbar item is clicked; starts a full online parse
     */
    private void refresh(MenuItem item) {
        //rotate the refresh button
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

        Animation rotation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.refresh_clockwise);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        refreshItem = item;

        //request full parsing process
        isOnlineRequested = true;

        //get data and put it into db, viewpager adapter is automatically refreshed
        new BgDownloader().execute(this);
    }

    /**
     * Called when the app is being paused and possibly closed; saves the currently requested vplan mode and appmode for next start-up
     */
    @Override
    protected void onPause() {

        //save the requested vplan mode and appmode for next startup
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PREF_VPLAN_MODE, requestedVplanMode);
        editor.putInt(PREF_APPMODE, appMode);
        editor.apply();

        super.onPause();
    }

    /**
     * Called when the settings sharedprefs were changed and calls refreshFilters()
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (sharedPreferences == getSharedPreferences(PREFS_NAME, 0)) {
            if (key.contentEquals(PREF_LAST_UPDATE)) activatePagerAdapter(this);
        }
        //settings have been changed, so update the filter array if the classes to filter have been changed
        refreshFilters();
    }

    /**
     * Refills the u/m/oinfo filters according to the saved values in settings prefs and refreshes the currently used filter
     */
    private void refreshFilters() {

        //create a new list and fill it
        ArrayList<String> filterUinfoTemp = new ArrayList<String>();
        ArrayList<String> filterMinfoTemp = new ArrayList<String>();
        ArrayList<String> filterOinfoTemp = new ArrayList<String>();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        keys = pref.getAll();
        String[] uinfoKeys = {getString(R.string.key_grade5), getString(R.string.key_grade6), getString(R.string.key_grade7)};

        String[] minfoKeys = {getString(R.string.key_grade8), getString(R.string.key_grade9), getString(R.string.key_grade10)};

        //iterate through all shared prefs stringsets
        int mode;

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            //skip pwd or uname
            if (entry.getKey().contentEquals(getString(R.string.key_uname)) || entry.getKey().contentEquals(getString(R.string.key_pwd))
                    || entry.getKey().contentEquals(getString(R.string.pref_key_upd_int)))
                continue;

            //treat bg updates separately
            if (entry.getKey().contentEquals(getString(R.string.pref_key_bg_updates))) {

                int interval = Integer.valueOf(pref.getString(getString(R.string.pref_key_upd_int), ""));

                refreshBgUpdates(Boolean.valueOf(entry.getValue().toString()), interval);

                continue;
            }

            if (Arrays.asList(uinfoKeys).contains(entry.getKey())) {
                mode = UINFO;
            } else if (Arrays.asList(minfoKeys).contains(entry.getKey())) {
                mode = MINFO;
            } else mode = OINFO;

            Set<String> set = pref.getStringSet(entry.getKey(), null);

            if (set == null || set.isEmpty()) continue;

            switch (mode) {

                case UINFO:
                    filterUinfoTemp.addAll(Arrays.asList(set.toArray(new String[set.size()])));
                    break;
                case MINFO:
                    filterMinfoTemp.addAll(Arrays.asList(set.toArray(new String[set.size()])));
                    break;
                case OINFO:
                    filterOinfoTemp.addAll(Arrays.asList(set.toArray(new String[set.size()])));
                    break;
            }

        }

        //activate the new filters
        filterUnterstufe = filterUinfoTemp;
        filterMittelstufe = filterMinfoTemp;
        filterOberstufe = filterOinfoTemp;

        //now determine which filter is currently needed
        switch (requestedVplanMode) {
            case UINFO:
                filterCurrent = filterUnterstufe;
                break;
            case MINFO:
                filterCurrent = filterMittelstufe;
                break;
            case OINFO:
                filterCurrent = filterOberstufe;
                break;
        }
    }

    private void refreshBgUpdates(Boolean activated, int interval) {

        //find out whether we have to update the pending intent
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        Boolean wasActiveBefore = pref.getBoolean(PREF_IS_BG_UPD_ACTIVE, false);
        int flag;
        if (wasActiveBefore) {
            flag = PendingIntent.FLAG_UPDATE_CURRENT;

            //only update the alarm if the interval really changed, else just skip this unless it has to be deactivated
            if (pref.getLong(PREF_CURR_BG_INT, 0) != interval || !activated) saveAlarm(activated, interval, flag, editor);
        } else saveAlarm(activated, interval, 0, editor);

        editor.apply();
    }

    private void saveAlarm(boolean activated, long interval, int flag, SharedPreferences.Editor editor) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent downloadIntent = new Intent(this, DownloaderService.class);
        PendingIntent pendingDownloadIntent = PendingIntent.getService(this, 0, downloadIntent, flag);

        if (activated) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval * AlarmManager.INTERVAL_HOUR, interval * AlarmManager.INTERVAL_HOUR, pendingDownloadIntent);

            //save that it is active now and also save the current interval
            editor.putBoolean(PREF_IS_BG_UPD_ACTIVE, true);
            editor.putLong(PREF_CURR_BG_INT, interval);
        } else {
            alarmManager.cancel(pendingDownloadIntent);

            //save that it isn't active anymore
            editor.putBoolean(PREF_IS_BG_UPD_ACTIVE, false);
        }
    }

    /**
     * Called when the grey statusbar displaying the filter status is clicked; shows a dialog displaying the contents of the current filter
     */
    public void onFilterInfoClick(View v) {

        final Context context = this;

        //show a dialog displaying the content of filterCurrent
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.curr_filter)
                .setItems(filterCurrent.toArray(new String[filterCurrent.size()]), null);

        //prepare the filter array list, if it is null then create a new one with a dummy item, else fill the dialog with the filter data
        if (filterCurrent.size() == 0) {
            builder.setItems(new CharSequence[]{getString(R.string.no_filter)}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //open settings to let the user add classes to the filter
                    startActivityForResult(new Intent(context, SettingsActivity.class), 0);
                    dialog.cancel();
                }
            });
        } else {
            builder.setItems(filterCurrent.toArray(new String[filterCurrent.size()]), null);
        }
        AlertDialog dialog = builder.show();
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

    public void activatePagerAdapter(Context context) {

        //activate adapter for viewPager
        VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), context, getParent(), filterCurrent);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(vplanPagerAdapter);

        //set a 1 dp margin between the fragments, filled with the divider_vertical drawable
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
        viewPager.setCurrentItem(getTodayVplanId(), false);
    }


    public void displayLastUpdate(String lastUpdate) {

        TextView lastUpdateTv = (TextView) findViewById(R.id.lastUpdate);
        lastUpdateTv.setText(lastUpdate);
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

    public void stopProgressBar() {

        //stop progress bar
        ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
        if (pb != null) {

            //if pb is null, then data is already displayed, so no dummy layout available
            pb.setIndeterminate(false);
            pb.setVisibility(View.GONE);
        }
    }

    public enum ProgressCode {STARTED, PARSING_FINISHED, FINISHED_ALL, ERR_NO_INTERNET, ERR_NO_CREDS, ERR_NO_INTERNET_OR_NO_CREDS}

    private class BgDownloader extends AsyncDownloader {

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
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {

            super.onPostExecute(success);
            if (success) {

                stopProgressBar();

                resetRefreshAnimation();

                //notify about success
                Toast.makeText(getApplicationContext(), getString(R.string.parsing_finished), Toast.LENGTH_SHORT).show();

                displayLastUpdate(refreshLastUpdate());

                activatePagerAdapter(context);
            }
        }
    }
}
