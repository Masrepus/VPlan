package com.masrepus.vplanapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;
import com.masrepus.vplanapp.constants.ProgressCode;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, Serializable, GoogleApiClient.ConnectionCallbacks {

    public static final java.text.DateFormat standardFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
    public static final String ACTIVITY_NAME = "MainActivity";
    public ArrayList<String> filterCurrent = new ArrayList<String>();
    public ArrayList<String> filterUnterstufe = new ArrayList<String>();
    public ArrayList<String> filterMittelstufe = new ArrayList<String>();
    public ArrayList<String> filterOberstufe = new ArrayList<String>();
    private int appMode;
    private int requestedVplanMode;
    private int requestedVplanId;
    private boolean isOnlineRequested;
    private VPlanDataSource datasource;
    private MenuItem refreshItem;
    private ActionBarDrawerToggle drawerToggle;
    private Map<String, ?> keys;
    private String currentVPlanLink;
    private int selectedVplanItem;
    private int selectedAppmodeItem;
    private GoogleApiClient apiClient;
    private ArrayList<DataMap> dataMaps;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        datasource = new VPlanDataSource(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //get the state of the filter from shared prefs
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        pref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                requestedVplanId = sharedPreferences.getInt(SharedPrefs.REQUESTED_VPLAN_ID, 0);
                currentVPlanLink = sharedPreferences.getString(SharedPrefs.CURR_VPLAN_LINK, "");
            }
        });
        requestedVplanMode = pref.getInt(SharedPrefs.VPLAN_MODE, VplanModes.UINFO);
        appMode = pref.getInt(SharedPrefs.APPMODE, AppModes.VPLAN);
        if (pref.getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false)) {
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
        datasource.open();
        if (!datasource.hasData(SQLiteHelperVplan.TABLE_LINKS)) {
            TextView welcome = (TextView) findViewById(R.id.welcome_textView);
            welcome.setVisibility(View.VISIBLE);
        }
        datasource.close();

        new PagerAdapterLoader().execute(this);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabStrip.setTabIndicatorColor(getResources().getColor(R.color.blue));

        //initialise navigation drawer
        TextView settings = (TextView) findViewById(R.id.textViewSettings);
        settings.setText(getString(R.string.settings).toUpperCase());

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_closed) {


                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);

                    //set title corresponding to requested vplan mode
                    switch (requestedVplanMode) {

                        case VplanModes.UINFO:
                            actionBar.setTitle(R.string.unterstufe);
                            break;
                        case VplanModes.MINFO:
                            actionBar.setTitle(R.string.mittelstufe);
                            break;
                        case VplanModes.OINFO:
                            actionBar.setTitle(R.string.oberstufe);
                            break;
                    }
                }

                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);

                    actionBar.setTitle(R.string.sgp);
                }
            };

            drawerLayout.setDrawerListener(drawerToggle);

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            //set the actionbar title matching to requestedVplanMode
            switch (requestedVplanMode) {

                case VplanModes.UINFO:
                    actionBar.setTitle(R.string.unterstufe);
                    break;
                case VplanModes.MINFO:
                    actionBar.setTitle(R.string.mittelstufe);
                    break;
                case VplanModes.OINFO:
                    actionBar.setTitle(R.string.oberstufe);
                    break;
            }
        }

        //display last update timestamp
        String lastUpdate = pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + appMode + requestedVplanMode, "");
        TextView tv = (TextView) findViewById(R.id.lastUpdate);
        tv.setVisibility(View.VISIBLE);
        tv.setText(lastUpdate);

        //prepare vplan mode listview
        ListView vplanModesLV = (ListView) findViewById(R.id.vplanModeList);

        //build the entries array
        String[] appModes = getResources().getStringArray(R.array.appmodes);
        String[] entries = DrawerListBuilder.addArrays(appModes, getResources().getStringArray(R.array.vplan_modes));

        //build the item list
        DrawerListBuilder listBuilder = new DrawerListBuilder(this, getResources().getStringArray(R.array.sectionHeaders), entries, 0, appModes.length);

        DrawerListAdapter modesAdapter = new DrawerListAdapter(this, this, listBuilder.getItems());

        vplanModesLV.setAdapter(modesAdapter);

        //restore last selected vplan item
        editor.putInt(SharedPrefs.SELECTED_VPLAN_ITEM, 1 + appModes.length + requestedVplanMode); //for uinfo and two appmodes, this must return 4
        editor.apply();


        //restore last selected appmode item
        editor.putInt(SharedPrefs.SELECTED_APPMODE_ITEM, 1 + appMode);
        editor.apply();

        modesAdapter.notifyDataSetChanged();

        //register change listener for settings sharedPrefs
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settingsPrefs.registerOnSharedPreferenceChangeListener(this);

        buildApiClient();
    }

    @Override
    public void onClick(View view) {

        //check whether this is an appmode item
        Integer appModeTag = (Integer) view.getTag(R.id.TAG_APPMODE);

        if (appModeTag != null) {

            //update the current appmode
            appMode = appModeTag;

            switch (appModeTag) {

                case AppModes.TESTS:
                    startActivity(new Intent(this, ExamsActivity.class).putExtra(Args.CALLING_ACTIVITY, ACTIVITY_NAME));
                    break;
                case AppModes.TIMETABLE:
                    startActivity(new Intent(this, TimetableActivity.class));
                    break;
            }

        } else {
            //check whether this is a vplan mode item or not
            Integer vplanMode = (Integer) view.getTag(R.id.TAG_VPLAN_MODE);
            Integer position = (Integer) view.getTag(R.id.TAG_POSITION);
            if (vplanMode != null) {

                //check whether there was a change in the selection
                if (requestedVplanMode != vplanMode) {

                    selectedVplanItem = position;
                    SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                    pref.edit().putInt(SharedPrefs.SELECTED_VPLAN_ITEM, selectedVplanItem).apply();

                    ListView vplanModes = (ListView) findViewById(R.id.vplanModeList);
                    DrawerListAdapter modesAdapter = (DrawerListAdapter) vplanModes.getAdapter();
                    modesAdapter.notifyDataSetChanged();

                    //change requested vplan mode and save it in shared prefs
                    requestedVplanMode = vplanMode;
                    getSharedPreferences(SharedPrefs.PREFS_NAME, 0).edit().putInt(SharedPrefs.VPLAN_MODE, requestedVplanMode).apply();

                    //select the right filter
                    switch (requestedVplanMode) {

                        case VplanModes.UINFO:
                            filterCurrent = filterUnterstufe;
                            break;
                        case VplanModes.MINFO:
                            filterCurrent = filterMittelstufe;
                            break;
                        case VplanModes.OINFO:
                            filterCurrent = filterOberstufe;
                            break;
                    }

                    //collapse the drawer
                    DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawerLayout.closeDrawers();

                    //recreate the pageradapter
                    ViewPager pager = (ViewPager) findViewById(R.id.pager);
                    pager.setAdapter(new LoadingAdapter(getSupportFragmentManager()));

                    //now start the adapter loading in a separate thread
                    new PagerAdapterLoader().execute(this);

                    //load the right last-update timestamp
                    String lastUpdate = getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getString(SharedPrefs.PREFIX_LAST_UPDATE + appMode + requestedVplanMode, "");
                    TextView tv = (TextView) findViewById(R.id.lastUpdate);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(lastUpdate);
                } //else just ignore the click
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        //Now the Wear Data Layer API can be used
        sendDataToWatch();
    }

    private void sendDataToWatch() {

        DataMap dataMap = new DataMap();

        //get the number of available days
        datasource.open();
        Cursor c = datasource.query(SQLiteHelperVplan.TABLE_LINKS, new String[]{SQLiteHelperVplan.COLUMN_ID});
        int count = c.getCount();

        for (int i = 0; i < count; i++) {
            dataMap.putDataMap(String.valueOf(i), fillDataMap(i));
        }

        new SendToDataLayerThread(DataKeys.VPLAN, dataMap).start();

        //now the headers
        dataMap = new DataMap();

        datasource.close();

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //get the header strings from shared prefs
        for (int i = 0; i < count; i++) {
            dataMap.putString(String.valueOf(i), pref.getString(SharedPrefs.PREFIX_VPLAN_CURR_DATE + String.valueOf(requestedVplanMode) + String.valueOf(i), ""));
        }

        new SendToDataLayerThread(DataKeys.HEADERS, dataMap).start();

        //send last updated timestamp and time published timestamps
        dataMap = new DataMap();

        String lastUpdate = pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + AppModes.VPLAN + requestedVplanMode, "");
        String[] timePublishedTimestamps = new String[count];

        for (int i = 0; i < count; i++) {

            //get each time published timestamp
            timePublishedTimestamps[i] = pref.getString(SharedPrefs.PREFIX_VPLAN_TIME_PUBLISHED + requestedVplanMode + i, "");
        }

        dataMap.putString(SharedPrefs.PREFIX_LAST_UPDATE, lastUpdate);
        dataMap.putStringArray(DataKeys.TIME_PUBLISHED_TIMESTAMPS, timePublishedTimestamps);
        dataMap.putInt(DataKeys.DAYS, count);

        new SendToDataLayerThread(DataKeys.META_DATA, dataMap).start();
    }

    private DataMap fillDataMap(int id) {

        DataMap dataMap = new DataMap();

        //query the data for the right vplan -> get requested table name by passed arg
        String tableName = SQLiteHelperVplan.tablesVplan[id];

        if (datasource.hasData(tableName)) {

            Cursor c = datasource.query(tableName, new String[]{SQLiteHelperVplan.COLUMN_ID, SQLiteHelperVplan.COLUMN_GRADE, SQLiteHelperVplan.COLUMN_STUNDE,
                    SQLiteHelperVplan.COLUMN_STATUS});

            ArrayList<Row> list = new ArrayList<Row>();
            ArrayList<Row> tempList = new ArrayList<Row>();

            //check whether filter is active
            Boolean isFilterActive = getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false);

            //if filter is active, then use it after filling the Arraylist
            int listSizeBeforeFilter = 0;
            if (isFilterActive) {

                while (c.moveToNext()) {
                    Row row = new Row();

                    //only add to list if row isn't null
                    String help = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE));
                    if (help.contentEquals("Klasse")) continue;
                    if (help.contentEquals("")) continue;

                    row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
                    row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));
                    row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));

                    tempList.add(row);
                }

                //now perform the filtering
                int position = 0;

                for (Row currRow : tempList) {
                    String klasse = currRow.getKlasse();
                    boolean isNeeded = false;

                    //look whether this row's klasse attribute contains any of the classes to filter for
                    if (filterCurrent.size() > 0) {
                        for (int i = 0; i <= filterCurrent.size() - 1; i++) {
                            char[] klasseFilter = filterCurrent.get(i).toCharArray();

                            //check whether this is oinfo, as in this case, the exact order of the filter chars must be given as well
                            if (requestedVplanMode == VplanModes.OINFO) {
                                String filterItem = filterCurrent.get(i);
                                isNeeded = klasse.contentEquals("Q" + filterItem);

                                if (isNeeded) break;
                                if (klasse.contentEquals("")) isNeeded = true;
                            } else { //in u/minfo the order doesn't play a role

                                //if klasse contains all of the characters of the filter string, isNeeded will be true, because if one character returns false, the loop is stopped
                                for (int y = 0; y <= klasseFilter.length - 1; y++) {
                                    if (klasse.contains(String.valueOf(klasseFilter[y]))) {
                                        isNeeded = true;
                                    } else {
                                        isNeeded = false;
                                        break;
                                    }
                                }
                                if (isNeeded) break;

                                //also set isneeded to true if klasse=""
                                if (klasse.contentEquals("")) isNeeded = true;
                            }
                        }
                    } else {
                        //if there is no item in the filter list, then still take the rows without a value for class
                        isNeeded = klasse.contentEquals("");
                    }
                    //if the test was positive, then add the current Row to the map
                    if (isNeeded) {
                        dataMap.putDataMap(String.valueOf(position), currRow.putToDataMap(new DataMap()));
                        position++;
                    }
                }

            } else {
                // just fill the list normally
                int position = 0;
                while (c.moveToNext()) {
                    Row row = new Row();

                    //only add to list if row isn't null
                    String help = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE));
                    if (help.contentEquals("Klasse")) continue;
                    if (help.contentEquals("")) continue;

                    row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
                    row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));
                    row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));

                    dataMap.putDataMap(String.valueOf(position), row.putToDataMap(new DataMap()));
                    position++;
                }
            }
        }

        return dataMap;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(getPackageName(), "onConnectionSuspended: " + cause);
    }

    private int getTodayVplanId() {

        //today's vplan id has been saved in sharedprefs
        SharedPreferences prefs = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        return prefs.getInt(SharedPrefs.TODAY_VPLAN, 0);

    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //refresh the pager adapter
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setCurrentItem(getTodayVplanId());
        appMode = AppModes.VPLAN;
        selectedAppmodeItem = 1 + appMode;
    }

    private void buildApiClient() {

        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(getPackageName(), "onConnectionFailed: " + connectionResult);
                    }
                })

                        //request access to the Wearable API
                .addApi(Wearable.API)
                .build();
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
        appMode = AppModes.VPLAN;

        ListView vplanModesLV = (ListView) findViewById(R.id.vplanModeList);
        DrawerListAdapter adapter = (DrawerListAdapter) vplanModesLV.getAdapter();
        adapter.notifyDataSetChanged();
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
            case VplanModes.BASIC:
                return vplanBase;
            case VplanModes.UINFO:
                url = vplanBase + "pw/" + "urekursiv.php";
                break;
            case VplanModes.MINFO:
                url = vplanBase + "pw/" + "mrekursiv.php";
                break;
            case VplanModes.OINFO:
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
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        if (pref.getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false)) {
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
            case R.id.tester:
                startService(new Intent(this, DownloaderService.class).putExtra(DataKeys.ACTION, Args.NOTIFY_WEAR_UPDATE_UI));
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
                    SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean(SharedPrefs.IS_FILTER_ACTIVE, false);
                    editor.apply();
                    fl.setVisibility(View.GONE);

                    //sync this new dataset to wear
                    sendDataToWatch();
                } else {
                    item.setChecked(true);
                    SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = pref.edit();

                    editor.putBoolean(SharedPrefs.IS_FILTER_ACTIVE, true);
                    editor.apply();
                    fl.setVisibility(View.VISIBLE);
                    filterWarning.setText(R.string.filter_enabled);

                    //sync to wear
                    sendDataToWatch();
                }
                //refresh adapter for viewPager
                new PagerAdapterLoader().execute(this);

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
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SharedPrefs.VPLAN_MODE, requestedVplanMode);
        editor.putInt(SharedPrefs.APPMODE, appMode);
        editor.apply();

        super.onPause();
    }

    /**
     * Called when the settings sharedprefs were changed and calls refreshFilters()
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (sharedPreferences == getSharedPreferences(SharedPrefs.PREFS_NAME, 0)) {
            if (key.contains(SharedPrefs.PREFIX_LAST_UPDATE)) activatePagerAdapter();
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

            //skip selected keys
            if (entry.getKey().contentEquals(getString(R.string.key_uname)) || entry.getKey().contentEquals(getString(R.string.key_pwd))
                    || entry.getKey().contentEquals(getString(R.string.pref_key_upd_int)) || entry.getKey().contentEquals(getString(R.string.pref_key_bg_upd_levels)))
                continue;

            //treat bg updates separately
            if (entry.getKey().contentEquals(getString(R.string.pref_key_bg_updates))) {

                int interval = Integer.valueOf(pref.getString(getString(R.string.pref_key_upd_int), ""));

                refreshBgUpdates(Boolean.valueOf(entry.getValue().toString()), interval);

                continue;
            }

            if (Arrays.asList(uinfoKeys).contains(entry.getKey())) {
                mode = VplanModes.UINFO;
            } else if (Arrays.asList(minfoKeys).contains(entry.getKey())) {
                mode = VplanModes.MINFO;
            } else mode = VplanModes.OINFO;

            Set<String> set = pref.getStringSet(entry.getKey(), null);

            if (set == null || set.isEmpty()) continue;

            switch (mode) {

                case VplanModes.UINFO:
                    filterUinfoTemp.addAll(Arrays.asList(set.toArray(new String[set.size()])));
                    break;
                case VplanModes.MINFO:
                    filterMinfoTemp.addAll(Arrays.asList(set.toArray(new String[set.size()])));
                    break;
                case VplanModes.OINFO:
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
            case VplanModes.UINFO:
                filterCurrent = filterUnterstufe;
                break;
            case VplanModes.MINFO:
                filterCurrent = filterMittelstufe;
                break;
            case VplanModes.OINFO:
                filterCurrent = filterOberstufe;
                break;
        }

        //save the filters in shared prefs
        pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        Set<String> unterstufeSet = new HashSet<String>(filterUnterstufe);
        Set<String> mittelstufeSet = new HashSet<String>(filterMittelstufe);
        Set<String> oberstufeSet = new HashSet<String>(filterOberstufe);
        editor.putStringSet(getString(R.string.pref_key_filter_uinfo), unterstufeSet)
                .putStringSet(getString(R.string.pref_key_filter_minfo), mittelstufeSet)
                .putStringSet(getString(R.string.pref_key_filter_oinfo), oberstufeSet)
                .apply();
    }

    private void refreshBgUpdates(Boolean activated, int interval) {

        //find out whether we have to update the pending intent
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        Boolean wasActiveBefore = pref.getBoolean(SharedPrefs.IS_BG_UPD_ACTIVE, false);
        int flag;
        if (wasActiveBefore) {
            flag = PendingIntent.FLAG_UPDATE_CURRENT;

            //only update the alarm if the interval really changed, else just skip this unless it has to be deactivated
            if (pref.getLong(SharedPrefs.CURR_BG_INT, 0) != interval || !activated)
                saveAlarm(activated, interval, flag, editor);
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
            editor.putBoolean(SharedPrefs.IS_BG_UPD_ACTIVE, true);
            editor.putLong(SharedPrefs.CURR_BG_INT, interval);
        } else {
            alarmManager.cancel(pendingDownloadIntent);

            //save that it isn't active anymore
            editor.putBoolean(SharedPrefs.IS_BG_UPD_ACTIVE, false);
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
        builder.show();
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

    public void activatePagerAdapter() {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new LoadingAdapter(getSupportFragmentManager()));
        pager.setOffscreenPageLimit(4);

        //activate adapter for viewPager
        new PagerAdapterLoader().execute(this);
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

    /**
     * PagerAdapter that only displays one dummy fragment containing a progressbar
     */
    private class LoadingAdapter extends FragmentStatePagerAdapter {

        public LoadingAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Bundle args = new Bundle();
            args.putBoolean(Args.VPLAN_LOADING_DUMMY, true);
            Fragment loadingFragment = new VplanFragment();
            loadingFragment.setArguments(args);

            return loadingFragment;
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    /**
     * Loads a VplanPagerAdapter in a background task
     */
    private class PagerAdapterLoader extends AsyncTask<Activity, Void, VplanPagerAdapter> {

        @Override
        protected VplanPagerAdapter doInBackground(Activity... activities) {
            return new VplanPagerAdapter(getSupportFragmentManager(), getApplicationContext(), activities[0], filterCurrent);
        }

        @Override
        protected void onPostExecute(VplanPagerAdapter vplanPagerAdapter) {

            //check whether disabling of welcome tv and activation of tabstrip must be done
            if (vplanPagerAdapter.hasData()) {

                TextView welcome = (TextView) findViewById(R.id.welcome_textView);
                welcome.setVisibility(View.GONE);

                PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
                tabStrip.setVisibility(View.VISIBLE);
            }

            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            pager.setAdapter(vplanPagerAdapter);

            //set a 1 dp margin between the fragments
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            pager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
            pager.setCurrentItem(getTodayVplanId(), false);

            //sync wear
            sendDataToWatch();
        }
    }

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

                activatePagerAdapter();

                sendDataToWatch();
            }
        }
    }

    private class SendToDataLayerThread extends Thread {

        private String path;
        private DataMap dataMap;
        private int failCount = 0;

        //Constructor for sending data objects to the data layer
        public SendToDataLayerThread(String path, DataMap dataMap) {
            this.path = path;
            this.dataMap = dataMap;
        }

        @Override
        public void run() {

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
            for (Node node : nodes.getNodes()) {

                //Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);

                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(apiClient, request).await();

                if (result.getStatus().isSuccess())
                    Log.v(getPackageName(), "DataMap: " + dataMap + "sent to: " + node.getDisplayName());
                else {
                    failCount++;
                    Log.e(getPackageName(), "ERROR: failed to send DataMap! (" + failCount + ")");

                    //retry later
                    try {
                        if (failCount <= 3) {
                            Thread.sleep(2000);
                        } //else stop it after 3 times trying
                    } catch (InterruptedException e) {}
                }
            }
        }
    }
}
