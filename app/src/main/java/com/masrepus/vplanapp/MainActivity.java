package com.masrepus.vplanapp;

import android.app.ActionBar;
import android.app.AlertDialog;
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
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
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

public class MainActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFS_NAME = "mPrefs";
    public static final String PREF_LAST_UPDATE = "lastUpdate";
    public static final String PREF_VPLAN_MODE = "mode";
    public static final String PREF_PREFIX_VPLAN_CURR_DATE = "currDate";
    public static final String PREF_PREFIX_VPLAN_TIME_PUBLISHED = "timePublished";
    public static final String PREF_IS_FILTER_ACTIVE = "isFilterActive";
    public static final String PREF_APPMODE = "appmode";
    public static final String PREF_TODAY_VPLAN = "todayVplan";
    private static final int BASIC = 0;
    public static final int UINFO = 1;
    public static final int MINFO = 2;
    public static final int OINFO = 3;
    public static int inflateStatus = 0;
    java.text.DateFormat format = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
    private int appMode;
    public static final int VPLAN = 0;
    public static final int TESTS = 1;
    private int requestedVplanMode;
    private int requestedVplanId;
    private boolean isOnlineRequested;
    private VPlanDataSource datasource;
    private MenuItem refreshItem;
    private ActionBarDrawerToggle drawerToggle;
    private String timePublished;
    public ArrayList<String> filterCurrent = new ArrayList<String>();
    public ArrayList<String> filterUnterstufe = new ArrayList<String>();
    public ArrayList<String> filterMittelstufe = new ArrayList<String>();
    public ArrayList<String> filterOberstufe = new ArrayList<String>();
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
        VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), this, filterCurrent);

        //if the adapter's number of fragments is 0, then display the no data layout on top of it

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(vplanPagerAdapter);

        //set a 1 dp margin between the fragments, filled with the divider_vertical drawable
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
        viewPager.setPageMarginDrawable(R.drawable.divider_vertical);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabStrip.setTabIndicatorColor(getResources().getColor(R.color.blue));

        //initialise navigation drawer
        TextView settings = (TextView) findViewById(R.id.textViewSettings);
        settings.setText(getString(R.string.settings).toUpperCase());

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        final ActionBar actionBar = getActionBar();
        final Context context = this;
        if (actionBar != null) {

            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_closed) {

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

                        //refresh adapter for viewPager
                        VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), context, filterCurrent);
                        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
                        viewPager.setAdapter(vplanPagerAdapter);

                        //set a 1 dp margin between the fragments, filled with the divider_vertical drawable
                        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                        viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
                        viewPager.setPageMarginDrawable(R.drawable.divider_vertical);
                    }
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    getActionBar().setTitle(R.string.sgp);

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
        /*RadioButton currAppmodeRadio = null;
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
        }*/

        //register change listener for settings sharedPrefs
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settingsPrefs.registerOnSharedPreferenceChangeListener(this);

    }

    private int getTodayVplanId() {

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_TODAY_VPLAN, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }

    public void onModeChangeRadioButtonClick(View v) {

        switch (v.getId()) {

            /*case R.id.radioTests:
                rbUiHandler(new int[]{R.id.radioTests, R.id.radioVPlan}, new int[]{R.id.radioTestsFrame, R.id.radioVPlanFrame});

                new TestsParse().execute(this);
                break;*/
            /*case R.id.radioVPlan:
                rbUiHandler(new int[]{R.id.radioVPlan, R.id.radioTests}, new int[]{R.id.radioVPlanFrame, R.id.radioTestsFrame});

                break;*/
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
        pager.setAdapter(new VplanPagerAdapter(getSupportFragmentManager(), this, filterCurrent));
    }

    /**
     * Called after everything is initialised and syncs the navigation drawer toggle in the actionbar
     *
     * @param savedInstanceState
     */
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setCurrentItem(getTodayVplanId());
    }

    /**
     * Checks whether the device is online and if this is true it fetches the currently available files from the right internet page by calling findRequestedVplan()
     */
    private void updateAvailableFilesList() throws Exception {

        //load the list of files that are available just now, if internet connection is available, else just skip that
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {

            //we are online

            //encode uname and pw for http post
            String encoding = encodeCredentials();

            if (encoding == null && requestedVplanMode != OINFO) throw new Exception("failed to connect without creds");

                Document doc = null;
                try {
                    doc = Jsoup.connect(findRequestedVPlan()).header("Authorization", "Basic " + encoding).post();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (encoding == null) throw new Exception("failed to connect oinfo");
                    else throw new Exception("failed to connect");
                }

                if (doc != null) {

                    //tempContent contains all found tables
                    Elements tempContent = doc.select("table[class=hyphenate]");
                    //first table is the wanted one for available files list
                    Elements availableFiles;

                    try {
                        availableFiles = tempContent.get(0).child(0).children();
                    } catch (Exception e) {
                        availableFiles = null;
                    }

                    //db input of available files + url's
                    if (availableFiles != null) {

                        int position = 0;
                        datasource.open();
                        datasource.newTable(MySQLiteHelper.TABLE_LINKS);

                        //now distribute the contents of availableFiles into a new list for the selection spinner
                        for (Element row : availableFiles) {
                            String url;
                            String tag;

                            tag = row.child(0).text();
                            url = row.child(0).child(0).attributes().get("href");

                            //sql insert
                            datasource.createRowLinks(position, tag, url);

                            position++;
                        }

                        datasource.close();
                    }
                }
        }
    }

    /**
     * Called in order to find the correct url for the currently requested vplan mode. To accomplish that, it passes the request on to getVPlanUrl with the current mode
     *
     * @return the url as a String
     */
    private String findRequestedVPlan() {

        //return the appropriate vplan for the requested mode, if it hasn't been initialised for any reason, return the uinfo url
        String url = "";

        switch (requestedVplanMode) {

            case UINFO:
                url = getVPlanUrl(UINFO, false);
                break;
            case MINFO:
                url = getVPlanUrl(MINFO, false);
                break;
            case OINFO:
                url = getVPlanUrl(OINFO, false);
                break;
        }

        if (url != "") return url;
        else return getVPlanUrl(UINFO, false);
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
                VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), this, filterCurrent);
                ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
                viewPager.setAdapter(vplanPagerAdapter);
                viewPager.setCurrentItem(getTodayVplanId());
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
        new BgParse().execute(this);
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
        int position = 0;

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            //skip pwd or uname
            if (entry.getKey().contentEquals(getString(R.string.key_uname)) || entry.getKey().contentEquals(getString(R.string.key_pwd)))
                continue;

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

            position++;

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

    private class TestsParse extends AsyncTask<Context, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Context... context) {

            //get the tests for q11
            Document doc;
            Elements tableRows;
            try {
                doc = Jsoup.connect(getString(R.string.vplan_base_url) + "oinfo/scha11.html").get();
            } catch (IOException e) {
                e.printStackTrace();
                doc = null;
                publishProgress(99);
            }
            if (doc == null) return false;

            publishProgress(1);

            try {
                tableRows = doc.select("table").first().child(0).children();
            } catch (Exception e) {
                e.printStackTrace();
                tableRows = null;
                publishProgress(999);
            }

            if (tableRows != null) {

                datasource.open();

                //clear the existing tests table
                datasource.newTable(MySQLiteHelper.TABLE_TESTS);

                String lastDate = null;

                for (Element row : tableRows) {

                    //skip the first row as it just contains the title
                    if (row.elementSiblingIndex() == 0) continue;

                    String[] columns = new String[row.children().size()];
                    String klasse;
                    String date = "";

                    //distribute the row's content into an array in order to get the columns
                    for (Element column : row.children()) {
                        columns[column.elementSiblingIndex()] = column.text();
                    }

                    //put the data into the right strings, no 1 is the date and no 2 is the class
                    if (columns.length > 0) {

                        //if the date column is empty, this means that it has already been used, so look for the last date
                        if (columns[0].contentEquals("")) {
                            if (lastDate != null && !lastDate.contentEquals("")) {
                                date = lastDate;
                            }
                        } else {
                            date = columns[0];
                            lastDate = date;
                        }

                        klasse = columns[1];

                        //sql insert the two values
                        datasource.createRowTests(date, klasse);
                    }
                }
                datasource.close();
            }

            Toast t = new Toast(context[0]);
            t.setText("TestsParse erfolgreich beendet");
            //TODO fehler

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (values[0]) {

                case 1:
                    //downloading has ended
                    break;
                case 99:
                    //error while downloading
                    break;
                case 999:
                    //error because there was no data to extract
                    break;
            }
        }
    }

    /**
     * Encodes the saved username and password with Base64 encoder
     *
     * @return the credentials as single, encoded String; null if there was any error
     */
    public String encodeCredentials() {
        //encode uname and pw for http post
        //uname and pwd are stored in settings sharedPrefs
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String uname = pref.getString(getString(R.string.key_uname), "");
        String pwd = pref.getString(getString(R.string.key_pwd), "");

        if (uname.contentEquals("") || pwd.contentEquals("")) return null;

        String creds = uname + ":" + pwd;

        byte[] data = null;
        try {
            data = creds.getBytes("UTF-8");
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            Log.e(getPackageName(), getString(R.string.err_getBytes));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(getPackageName(), getString(R.string.err_getBytes));
        }
        String encoding = Base64.encodeToString(data, Base64.DEFAULT);

        return encoding;
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

    /**
     * The async task used for background-parsing of online data
     */
    private class BgParse extends AsyncTask<Context, Integer, Boolean> {

        int progress;
        Context context;
        Toast progressToast;

        /**
         * Starts the process of parsing
         *
         * @param context Used for method calls that require a context parameter
         * @return returns true if everything went well
         */
        protected Boolean doInBackground(Context... context) {

            this.context = context[0];

            //download new data and then refresh pager adapter

            try {
                updateAvailableFilesList();
            } catch (Exception e) {
                //check whether this is because of missing creds
                if (e.getMessage() == "no creds available") {
                    publishProgress(9999);
                    return false;
                } else if (e.getMessage().contentEquals("failed to connect")) {
                    publishProgress(8888);
                    return false;
                } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                    publishProgress(8887);
                    return false;
                } else if (e.getMessage().contentEquals("failed to connect without creds")) {
                    publishProgress(8886);
                    return false;
                }
            }

            datasource.open();

            Cursor c = datasource.query(MySQLiteHelper.TABLE_LINKS, new String[]{MySQLiteHelper.COLUMN_URL});

            try {
                while (c.moveToNext()) {
                    //load every available vplan into the db
                    requestedVplanId = c.getPosition();
                    currentVPlanLink = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_URL));
                    parseDataToSql();
                }
                publishProgress(1);

                return true;
            } catch (Exception e) {

                //check whether this is because of missing creds
                if (e.getMessage() == "no creds available") {
                    publishProgress(9999);
                } else if (e.getMessage().contentEquals("failed to connect")) {
                    publishProgress(8888);
                } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                    publishProgress(8887);
                } else if (e.getMessage().contentEquals("failed to connect without creds")) {
                    publishProgress(8886);
                }
                else {
                    e.printStackTrace();
                }
                return false;
            }
        }

        /**
         * Called if the progress is updated
         *
         * @param values Only [0] in use: 0 is dowloading, 1 is download finished, 99 is error because no data available, 9999 is error because of missing credentials
         */
        @Override
        protected void onProgressUpdate(Integer... values) {

            switch (values[0]) {
                case 0:
                    progress = values[0];
                    progressToast = Toast.makeText(this.context, R.string.downloading, Toast.LENGTH_SHORT);
                    progressToast.show();
                    break;
                case 1:
                    progress = values[0];
                    if (progressToast != null) {
                        progressToast.cancel();
                    }
                    progressToast = Toast.makeText(this.context, R.string.download_finished, Toast.LENGTH_SHORT);
                    progressToast.show();
                    break;
                case 99:
                    progress = values[0];
                    if (progressToast != null) {
                        progressToast.cancel();
                    }
                    progressToast = Toast.makeText(this.context, R.string.no_data, Toast.LENGTH_LONG);
                    progressToast.show();
                    break;
                case 9999:
                    progress = values[0];
                    showAlert(context, R.string.no_creds, R.string.no_creds_detailed, 2);
                    break;
                case 8886:
                    progress = values[0];
                    showAlert(context, R.string.download_error_title, R.string.download_error_nocreds, 2);
                    break;
                case 8887:
                    progress = values[0];
                    showAlert(context, R.string.download_error_title, R.string.download_error_nointernet, 1);
                    break;
                case 8888:
                    progress = values[0];
                    showAlert(context, R.string.download_error_title, R.string.download_error, 1);
                    break;
            }
        }

        /**
         * Called when background parsing has finished and refreshes the whole ui, activates the adapter for the viewpager etc
         *
         * @param success is true if there was no error, false if an error occured, the user is being notified about that, unless error was because of missing credentials where the user has already been notified
         */
        @Override
        protected void onPostExecute(Boolean success) {
            if (success && progress != 99 && progress != 9999) {
                //stop progress bar
                ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
                if (pb != null) {

                    //if pb is null, then data is already displayed, so no dummy layout available
                    pb.setIndeterminate(false);
                    pb.setVisibility(View.INVISIBLE);
                } else {

                    //reset the refresh button to normal, non-rotating layout (if it has been initialised yet)
                    if (refreshItem != null) {
                        if (refreshItem.getActionView() != null) {
                            refreshItem.getActionView().clearAnimation();
                            refreshItem.setActionView(null);
                        }
                    }
                }

                //notify about success
                if (progressToast != null) {
                    progressToast.cancel();
                }
                progressToast = Toast.makeText(getApplicationContext(), getString(R.string.parsing_finished), Toast.LENGTH_SHORT);
                progressToast.show();

                //save and display last update timestamp
                Calendar calendar = Calendar.getInstance();
                String lastUpdate = format.format(calendar.getTime());

                SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(PREF_LAST_UPDATE, lastUpdate);
                editor.apply();

                TextView lastUpdateTv = (TextView) findViewById(R.id.lastUpdate);
                lastUpdateTv.setText(lastUpdate);

                //activate adapter for viewPager
                VplanPagerAdapter vplanPagerAdapter = new VplanPagerAdapter(getSupportFragmentManager(), context, filterCurrent);
                ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
                viewPager.setAdapter(vplanPagerAdapter);

                //set a 1 dp margin between the fragments, filled with the divider_vertical drawable
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                viewPager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
                viewPager.setPageMarginDrawable(R.drawable.divider_vertical);
                viewPager.setCurrentItem(getTodayVplanId());
            } else {
                ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
                if (pb != null) {

                    //if pb is null, then data is already displayed, so no dummy layout available
                    pb.setIndeterminate(false);
                    pb.setVisibility(View.INVISIBLE);
                }

                //reset the refresh button to normal, non-rotating layout (if it has been initialised yet)
                if (refreshItem != null) {
                    refreshItem.getActionView().clearAnimation();
                    refreshItem.setActionView(null);
                }

                //check what the cause for the error was
                if (progress == 99) {
                    //notify about no data available
                    Toast.makeText(getApplicationContext(), getString(R.string.no_data), Toast.LENGTH_SHORT).show();
                }
                //if progress is 9999 or 8888, then the user has already been notified about the error, so no further action needed

            }
        }


        /**
         * Takes care of all the downloading and db-inserting
         *
         * @throws Exception is thrown only if the returned encoding from encodeCredentials() was null
         */
        public void parseDataToSql() throws Exception {

            String encoding = encodeCredentials();

            if (encoding == null && requestedVplanMode != OINFO) throw new Exception("no creds available");

            Document doc;

            //load vplan xhtml file and select the right table into Elements table
            try {
                doc = Jsoup.connect(findRequestedVPlan()).header("Authorization", "Basic " + encoding).post();
            } catch (Exception e) {
                if (encoding == null) {
                    if (requestedVplanMode != OINFO) throw new Exception("failed to connect without creds");
                    else throw new Exception("failed to connect oinfo");
                } else throw new Exception("failed to connect");
            }

            //tempContent contains all found tables
            Elements tempContent = doc.select("table[class=hyphenate]");
            //the desired table is second child of tempContent
            //each child of tableRows is one table row:
            Element headerCurrentDate;
            Elements tableRows;
            Elements availableFiles;
            try {
                tableRows = tempContent.get(1).child(0).children();
            } catch (Exception e) {
                tableRows = null;
            }

            //put the text of the h2 element after availableFiles table into headerCurrentDate
            try {
                headerCurrentDate = doc.select("h2").first();
            } catch (Exception e) {
                headerCurrentDate = null;
            }

            //only take the current day of the week and the date out of the header text; delete the space before the split string
            String[] separated = null;
            if (headerCurrentDate != null) {
                separated = headerCurrentDate.text().split("für");
            }
            String currentDate = null;
            if (separated != null) {
                if (separated.length <= 2) currentDate = separated[1].trim();
                else currentDate = separated[2].trim();
            }

            //get timePublished timestamp
            try {
                timePublished = doc.select("p").first().text();
            } catch (Exception e) {
                timePublished = "";
            }


            //now save the current loaded vplan's date and its last-changed timestamp for later usage
            SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(PREF_PREFIX_VPLAN_CURR_DATE + String.valueOf(requestedVplanId), currentDate);
            editor.putString(PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanId), timePublished);
            editor.apply();

            //put the contents of the first table (available files) into another elements object for further processing
            try {
                availableFiles = tempContent.get(0).children().first().children();
            } catch (Exception e) {
                availableFiles = null;
            }

            if (tableRows != null) {
                int position = 0;

                datasource.open();

                //clear the existing table for the requested vplan
                switch (requestedVplanId) {
                    case 0:
                        datasource.newTable(MySQLiteHelper.TABLE_VPLAN_0);
                        break;
                    case 1:
                        datasource.newTable(MySQLiteHelper.TABLE_VPLAN_1);
                        break;
                    case 2:
                        datasource.newTable(MySQLiteHelper.TABLE_VPLAN_2);
                        break;
                    case 3:
                        datasource.newTable(MySQLiteHelper.TABLE_VPLAN_3);
                        break;
                    case 4:
                        datasource.newTable(MySQLiteHelper.TABLE_VPLAN_4);
                        break;
                }

                for (Element row : tableRows) {


                    String[] columns = new String[row.children().size()];
                    String stunde = null;
                    String klasse = null;
                    String status = null;

                    //distribute the row's content into an array in order to get the columns
                    for (int c = 0; c + 1 <= row.children().size(); c++) {
                        columns[c] = row.child(c).text();
                    }

                    //put the data into the right strings, all strings from columns >= column 3 into status column
                    if (columns.length > 1) {
                        klasse = columns[0];
                        stunde = columns[1];

                        if (columns.length > 2) {
                            //insert the third column into status, then add all the remaining columns to it as well
                            status = columns[2];
                        } else status = "";
                    }

                    for (int i = 3; i + 1 <= row.children().size(); i++) {
                        status += " ";
                        status += columns[i];
                    }


                    //sql insert of all three columns, but only if they aren't all empty
                    if (stunde != null && !klasse.contentEquals("Klasse")) {

                        switch (requestedVplanId) {
                            case 0:
                                datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_0, position, stunde, klasse, status);
                                break;
                            case 1:
                                datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_1, position, stunde, klasse, status);
                                break;
                            case 2:
                                datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_2, position, stunde, klasse, status);
                                break;
                            case 3:
                                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_3);
                                datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_3, position, stunde, klasse, status);
                                break;
                            case 4:
                                datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_4, position, stunde, klasse, status);
                                break;
                        }

                    }

                    position++;
                }
                datasource.close();
            } else {
                publishProgress(99);
            }

            if (availableFiles != null) {

                int position = 0;
                datasource.open();
                datasource.newTable(MySQLiteHelper.TABLE_LINKS);

                //now distribute the contents of availableFiles into a new list for the selection spinner
                for (Element row : availableFiles) {
                    String url;
                    String tag;

                    tag = row.child(0).text();
                    url = row.child(0).child(0).attributes().get("href");

                    //sql insert
                    datasource.createRowLinks(position, tag, url);

                    position++;
                }

                datasource.close();
            }
        }
    }
}
