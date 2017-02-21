package com.masrepus.vplanapp.vplan;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.masrepus.vplanapp.CollectionTools;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Tutorial;
import com.masrepus.vplanapp.network.AsyncDownloader;
import com.masrepus.vplanapp.network.DownloaderService;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;
import com.masrepus.vplanapp.constants.ProgressCode;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperVplan;
import com.masrepus.vplanapp.exams.ExamsActivity;
import com.masrepus.vplanapp.settings.SettingsActivity;
import com.masrepus.vplanapp.timetable.TimetableActivity;
import com.rampo.updatechecker.UpdateChecker;
import com.rampo.updatechecker.store.Store;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, Serializable, GoogleApiClient.ConnectionCallbacks, NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {

    public static final java.text.DateFormat standardFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
    public static final String ACTIVITY_NAME = "MainActivity";
    public ArrayList<String> filterCurrent = new ArrayList<>();
    public ArrayList<String> filterUnterstufe = new ArrayList<>();
    public ArrayList<String> filterMittelstufe = new ArrayList<>();
    public ArrayList<String> filterOberstufe = new ArrayList<>();
    private ArrayList<String> customFilterCurrent = new ArrayList<>();
    private ArrayList<String> mergedFilter = new ArrayList<>();
    private int appMode;
    private int requestedVplanMode;
    private int requestedVplanId;
    private DataSource datasource;
    private MenuItem refreshItem;
    private ActionBarDrawerToggle drawerToggle;
    private Map<String, ?> keys;
    private String currentVPlanLink;
    private GoogleApiClient apiClient;
    private ShowcaseView showcase;
    private boolean tutorialMode;
    private boolean darkTheme;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        datasource = new DataSource(this);

        super.onCreate(savedInstanceState);

        //set dark theme if requested
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.ThemeDark);
            darkTheme = true;
        } else darkTheme = false;
        setContentView(R.layout.activity_main);

        //get the state of the filter from shared prefs
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        pref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            requestedVplanId = sharedPreferences.getInt(SharedPrefs.REQUESTED_VPLAN_ID, 0);
            currentVPlanLink = sharedPreferences.getString(SharedPrefs.CURR_VPLAN_LINK, "");
        });

        loadDbData(pref, editor);

        //start loading the pager adapter asynchronously
        new PagerAdapterLoader().execute(this);
        NavigationView drawer = initDrawer();

        //display last update timestamp
        String lastUpdate = getString(R.string.last_update) + " " + pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + appMode + requestedVplanMode, "");
        drawer.getMenu().findItem(R.id.lastUpdate).setTitle(lastUpdate);

        //register change listener for settings sharedPrefs
        SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settingsPrefs.registerOnSharedPreferenceChangeListener(this);

        buildApiClient();

        //check if the tutorial has never been shown yet
        tutorialMode = !pref.getBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "MainActivity", false);

        if (tutorialMode) {
            askAboutTutorial();
        }

        //check for updates
        UpdateChecker checker = new UpdateChecker(this);
        checker.setStore(Store.GOOGLE_PLAY);
        checker.setSuccessfulChecksRequired(2);
        checker.start();
    }

    private void loadDbData(SharedPreferences pref, SharedPreferences.Editor editor) {
        datasource.open();
        //delete the urls in linktable if this the first time running after the update(resolve crash)
        if (18 > pref.getInt(SharedPrefs.LAST_VERSION_RUN, 0)) {
            datasource.newTable(SQLiteHelperVplan.TABLE_LINKS);
        }
        try {
            //save the current version code in shared prefs
            editor.putInt(SharedPrefs.LAST_VERSION_RUN, getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
            editor.apply();
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        requestedVplanMode = pref.getInt(SharedPrefs.VPLAN_MODE, VplanModes.UINFO);
        appMode = pref.getInt(SharedPrefs.APPMODE, AppModes.VPLAN);
        if (pref.getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false)) {
            FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);
            fl.setVisibility(View.VISIBLE);
            TextView filterWarning = (TextView) findViewById(R.id.filterWarning);
            filterWarning.setText(R.string.filter_enabled);
        }

        refreshFilters();

        //prevent crash if linktable not present
        try {
            datasource.hasData(SQLiteHelperVplan.TABLE_LINKS);
        } catch (Exception e) {
            //no data there and no table to be found, fix this
            datasource.newTable(SQLiteHelperVplan.TABLE_LINKS);
        }

        //activate adapter for viewPager
        if (!datasource.hasData(SQLiteHelperVplan.TABLE_LINKS)) {
            TextView welcome = (TextView) findViewById(R.id.welcome_textView);
            welcome.setVisibility(View.VISIBLE);
        }
        datasource.close();
    }

    @NonNull
    private NavigationView initDrawer() {
        //initialise navigation drawer
        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.setNavigationItemSelectedListener(this);
        drawer.getMenu().findItem(R.id.vplan_appmode_item).setChecked(true);

        //check the current vplanmode item
        switch (requestedVplanMode) {

            case VplanModes.UINFO:
                drawer.getMenu().findItem(R.id.unterstufe_item).setChecked(true);
                break;
            case VplanModes.MINFO:
                drawer.getMenu().findItem(R.id.mittelstufe_item).setChecked(true);
                break;
            case VplanModes.OINFO:
                drawer.getMenu().findItem(R.id.oberstufe_item).setChecked(true);
                break;
        }

        //display current app info in appinfo nav item
        MenuItem appInfo = drawer.getMenu().findItem(R.id.appInfo);
        try {
            appInfo.setTitle("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " by Samuel Hopstock");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appInfo.setTitle("Fehler");
        }

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            initDrawerToggle(drawerLayout, toolbar, actionBar);

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
        return drawer;
    }

    private void initDrawerToggle(final DrawerLayout drawerLayout, final Toolbar toolbar, final ActionBar actionBar) {
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

                if (tutorialMode) {
                    refreshTutorial();
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                actionBar.setTitle(R.string.sgp);

                //if we are in tutorial mode continue giving instructions
                if (tutorialMode) {

                    //if showcase is null, init it
                    if (showcase == null) {
                        showcase = new ShowcaseView.Builder(MainActivity.this)
                                .setStyle(R.style.ShowcaseTheme)
                                .build();
                    }
                    showcase.setButtonPosition(getRightParam(getResources()));
                    showcase.setTarget(Target.NONE);
                    showcase.setContentTitle(getString(R.string.tut_drawer_title));
                    showcase.setContentText(getString(R.string.tut_drawer_text));
                    showcase.setButtonText(getString(R.string.next));
                    showcase.setShouldCentreText(true);
                    showcase.overrideButtonClick(v -> {
                        DrawerLayout layout = (DrawerLayout) findViewById(R.id.drawer_layout);
                        layout.closeDrawers();
                    });
                }
            }
        };
    }

    private int getColor(Resources res, int id, Resources.Theme theme) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return res.getColor(id, theme);
        } else {
            return res.getColor(id);
        }
    }

    private void refreshTutorial() {
        //tell the user how to refresh vplan data and then take him over to the overflow menu in order to add some classes to his filter
        if (showcase == null) {
            showcase = new ShowcaseView.Builder(this).build();
            showcase.setButtonPosition(getRightParam(getResources()));
            showcase.setButtonText(getString(R.string.next));
            showcase.setStyle(R.style.ShowcaseTheme);
        }

        showcase.setTarget(new ViewTarget(findViewById(R.id.action_refresh)));
        showcase.setContentTitle(getString(R.string.tut_refresh_title));
        showcase.setContentText(getString(R.string.tut_refresh_text));
        showcase.overrideButtonClick(v -> {
            showcase.hide();
            View refresh = findViewById(R.id.action_refresh);
            refresh.performClick();
        });
    }

    private void overflowTutorial() {
        //continue to the settings menu and tell the user if the device has a hardware menu button

        if (ViewConfiguration.get(MainActivity.this).hasPermanentMenuKey()) {
            if (showcase == null) {
                showcase = new ShowcaseView.Builder(this)
                        .setStyle(R.style.ShowcaseTheme)
                        .build();
                showcase.setButtonPosition(getRightParam(getResources()));
            }

            showcase.setTarget(Target.NONE);
            showcase.setContentTitle(getString(R.string.filter));
            showcase.setContentText(getString(R.string.tut_filter_hardware_menu));
            showcase.setShouldCentreText(false);
            showcase.overrideButtonClick(v -> {
                showcase.hide();
                MainActivity.this.openOptionsMenu();
            });
        } else {
            if (showcase == null) {
                showcase = new ShowcaseView.Builder(this)
                        .setStyle(R.style.ShowcaseTheme)
                        .build();
                showcase.setButtonPosition(getRightParam(getResources()));
            }

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            final List<View> views = toolbar.getTouchables();
            ViewTarget target = new ViewTarget(views.get(views.size() - 1)); //overflow

            showcase.setTarget(target);
            showcase.setContentTitle(getString(R.string.filter));
            showcase.setContentText(getString(R.string.tut_filter_no_menubutton));
            showcase.setShouldCentreText(false);
            showcase.setHideOnTouchOutside(true);
            showcase.overrideButtonClick(v -> {
                showcase.hide();
                views.get(views.size() - 1).performClick();
            });
        }
        showcase.show();
        tutorialMode = false;
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        pref.edit().putBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "MainActivity", true).apply();
        pref.edit().putBoolean(SharedPrefs.TUT_RUNNING, false).apply();
    }

    @Override
    public void onClick(View view) {
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
        if (count > 5) count = 5; //max 5 items!

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

            ArrayList<Row> tempList = new ArrayList<>();

            //check whether filter is active
            Boolean isFilterActive = getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false);

            //if filter is active, then use it after filling the Arraylist
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
                dataMap = filterRowsToDataMap(dataMap, tempList);

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

    private DataMap filterRowsToDataMap(DataMap dataMap, ArrayList<Row> tempList) {
        int position = 0;

        for (Row currRow : tempList) {
            String klasse = currRow.getKlasse();
            boolean isNeeded = false;

            //look whether this row's klasse attribute contains any of the classes to filter for
            if (mergedFilter.size() > 0) {
                for (int i = 0; i <= mergedFilter.size() - 1; i++) {
                    char[] klasseFilter = mergedFilter.get(i).toCharArray();

                    //check whether this is oinfo, as in this case, the exact order of the filter chars must be given as well
                    if (requestedVplanMode == VplanModes.OINFO) {
                        String filterItem = mergedFilter.get(i);
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

        return dataMap;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("Google Services", "onConnectionSuspended: " + cause);
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

        //refresh the drawer
        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.getMenu().findItem(R.id.vplan_appmode_item).setChecked(true);
        drawer.getMenu().findItem(R.id.exams_appmode_item).setChecked(false);
        drawer.getMenu().findItem(R.id.timetable_appmode_item).setChecked(false);

        //set the appmode to vplan
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SharedPrefs.APPMODE, AppModes.VPLAN);
        editor.apply();
    }

    private void buildApiClient() {

        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(connectionResult -> Log.d("Google Services", "onConnectionFailed: " + connectionResult))

                        //request access to the Wearable API
                .addApi(Wearable.API)
                .build();
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
        pager.setAdapter(null);
        pager.setAdapter(new VplanPagerAdapter(getSupportFragmentManager(), this, this, mergedFilter));
        appMode = AppModes.VPLAN;
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

        tintIcons(menu);

        return super.onCreateOptionsMenu(menu);
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
            /*case R.id.tester:
                startService(new Intent(this, DownloaderService.class).putExtra(DataKeys.ACTION, Args.NOTIFY_WEAR_UPDATE_UI));
                return true;*/
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
                    //tell the user how the filter works if the current filter is still empty
                    if (filterCurrent.isEmpty()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.filter))
                                .setMessage(getString(R.string.filter_explanations))
                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                    dialog.dismiss();
                                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 0);
                                })
                                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
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
                }
                //refresh adapter for viewPager
                new PagerAdapterLoader().execute(this);
                return true;
            case R.id.action_help:
                SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                pref.edit().putBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "MainActivity", true).apply();
                tutorialMode = true;
                askAboutTutorial();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void askAboutTutorial() {

        //first ask the user if he really wants to start the tutorial now
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tutorial))
                .setMessage(getString(R.string.msg_start_tutorial))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    //disable the tutorial mode
                    SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                    pref.edit().putBoolean(SharedPrefs.TUT_SHOWN_PREFIX + "MainActivity", true).apply();
                    tutorialMode = false;
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    showTutorial();
                    dialog.dismiss();
                }).show();
    }

    private void showTutorial() {

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        pref.edit().putBoolean(SharedPrefs.TUT_RUNNING, true).apply();

        showcase = new ShowcaseView.Builder(this)
                .setTarget(new PointTarget(16, 16))
                .setContentTitle(getString(R.string.tut_start_title))
                .setContentText(getString(R.string.tut_start_text))
                .setStyle(R.style.ShowcaseTheme)
                .build();
        showcase.setButtonPosition(getRightParam(getResources()));
        showcase.setButtonText(getString(R.string.next));
        showcase.overrideButtonClick(v -> {
            DrawerLayout layout = (DrawerLayout) findViewById(R.id.drawer_layout);
            layout.openDrawer(findViewById(R.id.drawer_left));
        });
        showcase.setBlocksTouches(false);
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

    public RelativeLayout.LayoutParams getRightParam(Resources res) {
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = ((Number) (res.getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, getNavigationBarHeight(res.getConfiguration().orientation
        ) + (int) res.getDisplayMetrics().density * 10);
        return lps;
    }

    public void refresh(View v) {
        refresh(refreshItem);
    }

    /**
     * Called when the refresh actionbar item is clicked; starts a full online parse
     */
    private void refresh(MenuItem item) {

        //get the tint color
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.tintMenu, typedValue, true);
        int color = typedValue.data;

        //rotate the refresh button
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.view_action_refresh, null);

        Animation rotation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.refresh_clockwise);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        refreshItem = item;

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
        editor.putInt(SharedPrefs.APPMODE, AppModes.VPLAN);
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

        //if this was a theme change, restart
        if (darkTheme != sharedPreferences.getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            finish();
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        //settings have been changed, so update the filter array if the classes to filter have been changed
        refreshFilters();
    }

    /**
     * Refills the u/m/oinfo filters according to the saved values in settings prefs and refreshes the currently used filter
     */
    private void refreshFilters() {

        //create a new list and fill it
        ArrayList<String> filterUinfoTemp = new ArrayList<>();
        ArrayList<String> filterMinfoTemp = new ArrayList<>();
        ArrayList<String> filterOinfoTemp = new ArrayList<>();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        keys = pref.getAll();
        String[] uinfoKeys = {getString(R.string.key_grade5), getString(R.string.key_grade6), getString(R.string.key_grade7)};

        String[] minfoKeys = {getString(R.string.key_grade8), getString(R.string.key_grade9), getString(R.string.key_grade10)};

        //iterate through all shared prefs stringsets and delete the still existing old keys
        int mode;

        SharedPreferences mPref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        //get the keys used in settings or skip if not available
        ArrayList<String> usedKeys = new ArrayList<>(mPref.getStringSet(SharedPrefs.USED_SETTINGS_KEYS, new HashSet<>()));

        if (usedKeys.size() > 0) {
            //find the differences between used keys and keys present in sharedPrefs file
            ArrayList<String> differences = new ArrayList<>(CollectionTools.nonOverLap(usedKeys, keys.keySet()));
            //the old keys are those contained in differences and also keys.keySet
            ArrayList<String> oldKeys = new ArrayList<>(CollectionTools.intersect(keys.keySet(), differences));

            //remove username, pwd, custom classes and update keys from the list if present
            oldKeys.remove(getString(R.string.key_uname));
            oldKeys.remove(getString(R.string.key_pwd));
            oldKeys.remove(getString(R.string.pref_key_bg_updates));
            oldKeys.remove(getString(R.string.pref_key_upd_int));
            oldKeys.remove(getString(R.string.pref_key_bg_upd_levels));
            oldKeys.remove(SharedPrefs.CUSTOM_CLASSES_PREFIX + VplanModes.UINFO);
            oldKeys.remove(SharedPrefs.CUSTOM_CLASSES_PREFIX + VplanModes.MINFO);
            oldKeys.remove(SharedPrefs.CUSTOM_CLASSES_PREFIX + VplanModes.OINFO);

            //now delete those old keys from settings prefs
            SharedPreferences.Editor editor = pref.edit();
            for (String oldKey : oldKeys) {
                editor.remove(oldKey);
                editor.apply();
            }
        }

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

            //treat dark theme settings separately
            if (entry.getKey().contentEquals(getString(R.string.pref_key_dark_theme))) {
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

        //save the filters in main shared prefs
        SharedPreferences.Editor editor = mPref.edit();
        Set<String> unterstufeSet = new HashSet<>(filterUnterstufe);
        Set<String> mittelstufeSet = new HashSet<>(filterMittelstufe);
        Set<String> oberstufeSet = new HashSet<>(filterOberstufe);
        editor.putStringSet(getString(R.string.pref_key_filter_uinfo), unterstufeSet)
                .putStringSet(getString(R.string.pref_key_filter_minfo), mittelstufeSet)
                .putStringSet(getString(R.string.pref_key_filter_oinfo), oberstufeSet)
                .apply();

        refreshCustomFilter(pref);
    }

    private void refreshCustomFilter(SharedPreferences pref) {

        //get currently saved custom classes
        HashSet<String> customClasses = new HashSet<>(pref.getStringSet(SharedPrefs.CUSTOM_CLASSES_PREFIX + requestedVplanMode, new HashSet<>()));

        //keep a local copy of the custom items for later
        customFilterCurrent = new ArrayList<>(customClasses);

        //merge filterCurrent and custom filter
        mergedFilter = new ArrayList<>();
        mergedFilter.addAll(filterCurrent);
        mergedFilter.addAll(customFilterCurrent);
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
                .setItems(mergedFilter.toArray(new String[mergedFilter.size()]), null);

        //prepare the filter array list, if it is null then create a new one with a dummy item, else fill the dialog with the filter data
        if (mergedFilter.size() == 0) {
            builder.setItems(new CharSequence[]{getString(R.string.no_filter)}, (dialog, which) -> {
                //open settings to let the user add classes to the filter
                startActivityForResult(new Intent(context, SettingsActivity.class), 0);
                dialog.cancel();
            });
        } else {
            builder.setItems(mergedFilter.toArray(new String[mergedFilter.size()]), null);
        }

        //add an add class/course button for custom elements
        builder.setPositiveButton(getString(R.string.add_missing_class), (dialog, which) -> {
            dialog.dismiss();

            //show a dialog where the user can enter a custom class
            View dialogView = View.inflate(MainActivity.this, R.layout.dialog_add_class, null);
            AlertDialog.Builder childBuilder = new AlertDialog.Builder(MainActivity.this);
            childBuilder.setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (dialog1, which1) -> {
                        saveClass((AlertDialog) dialog1);
                        dialog1.dismiss();
                        refreshFilters();
                        //refresh the adapter
                        new PagerAdapterLoader().execute(MainActivity.this);
                    });
            AlertDialog childDialog = childBuilder.create();
            childDialog.setView(dialogView);
            childDialog.show();
        });
        final AlertDialog parentDialog = builder.create();
        parentDialog.getListView().setOnItemLongClickListener((parent, view, position, id) -> {
            //check if this is a custom item
            final String customClass = mergedFilter.get(position);

            if (customFilterCurrent.contains(mergedFilter.get(position))) {

                //custom item, prompt option to delete it
                AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                deleteDialogBuilder.setMessage(R.string.remove_custom_class)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            removeClass(customClass);
                            refreshFilters();
                            parentDialog.dismiss();
                            new PagerAdapterLoader().execute(MainActivity.this);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
            return true;
        });
        parentDialog.show();
    }

    private void removeClass(String customClass) {

        //get the currently saved custom classes
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = pref.edit();
        HashSet<String> customClasses = new HashSet<>(pref.getStringSet(SharedPrefs.CUSTOM_CLASSES_PREFIX + requestedVplanMode, new HashSet<>()));

        //now remove the requested class
        customClasses.remove(customClass);

        //save
        editor.putStringSet(SharedPrefs.CUSTOM_CLASSES_PREFIX + requestedVplanMode, customClasses);
        editor.apply();
    }

    public void saveClass(AlertDialog dialog) {

        //get the entered class
        String customClass = ((EditText)dialog.findViewById(R.id.classEditText)).getText().toString();
        if (customClass.isEmpty()) {
            Toast.makeText(MainActivity.this, R.string.input_empty, Toast.LENGTH_LONG).show();
            return;
        }

        //remove leading Q if needed
        if (customClass.charAt(0) == 'Q' && customClass.length() > 1) customClass = customClass.substring(1);

        //add the saved class to sharedprefs
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = pref.edit();

        //get currently saved custom classes and add the one the user wants to save
        HashSet<String> customClasses = new HashSet<>(pref.getStringSet(SharedPrefs.CUSTOM_CLASSES_PREFIX + requestedVplanMode, new HashSet<>()));
        customClasses.add(customClass);

        //keep a local copy of the custom items for later
        customFilterCurrent = new ArrayList<>(customClasses);

        editor.putStringSet(SharedPrefs.CUSTOM_CLASSES_PREFIX + requestedVplanMode, customClasses);
        editor.apply();
    }

    public void showAlert(final Context context, int titleStringRes, int msgStringRes, int buttonCount) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleStringRes)
                .setMessage(msgStringRes);

        //if buttoncount is 1, then there should be only one ok button, otherwise also add a cancel one
        switch (buttonCount) {
            case 1:
                builder.setNegativeButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                break;
            case 2:
                builder.setPositiveButton(R.string.ok, (dialog, which) -> startActivityForResult(new Intent(context, SettingsActivity.class), 0))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                break;
        }

        builder.show();
    }

    public void activatePagerAdapter() {

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(null);
        pager.setAdapter(new LoadingAdapter(getSupportFragmentManager()));
        pager.setOffscreenPageLimit(4);

        //activate adapter for viewPager
        new PagerAdapterLoader().execute(this);
    }

    public void displayLastUpdate(String lastUpdate) {

        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        drawer.getMenu().findItem(R.id.lastUpdate).setTitle(lastUpdate);
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

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
        Menu menu = drawer.getMenu();

        switch (menuItem.getItemId()) {

            case R.id.exams_appmode_item:
                menu.findItem(R.id.vplan_appmode_item).setChecked(false);
                startActivity(new Intent(this, ExamsActivity.class).putExtra(Args.CALLING_ACTIVITY, ACTIVITY_NAME));
                break;
            case R.id.timetable_appmode_item:
                menu.findItem(R.id.vplan_appmode_item).setChecked(false);
                startActivity(new Intent(this, TimetableActivity.class).putExtra(Args.CALLING_ACTIVITY, ACTIVITY_NAME));
                break;
            case R.id.unterstufe_item:
                changeVplanMode(VplanModes.UINFO);
                menu.findItem(R.id.mittelstufe_item).setChecked(false);
                menu.findItem(R.id.oberstufe_item).setChecked(false);
                break;
            case R.id.mittelstufe_item:
                changeVplanMode(VplanModes.MINFO);
                menu.findItem(R.id.unterstufe_item).setChecked(false);
                menu.findItem(R.id.oberstufe_item).setChecked(false);
                break;
            case R.id.oberstufe_item:
                changeVplanMode(VplanModes.OINFO);
                menu.findItem(R.id.mittelstufe_item).setChecked(false);
                menu.findItem(R.id.unterstufe_item).setChecked(false);
                break;
            case R.id.settings_item:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                break;
        }
        return true;
    }

    private void changeVplanMode(int vplanMode) {

        //check whether there was a change in the selection
        if (requestedVplanMode != vplanMode) {

            SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

            //change requested vplan mode and save it in shared prefs
            requestedVplanMode = vplanMode;
            pref.edit().putInt(SharedPrefs.VPLAN_MODE, requestedVplanMode).apply();

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

            //also refresh the custom filter
            refreshCustomFilter(PreferenceManager.getDefaultSharedPreferences(this));

            //collapse the drawer
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawers();

            //recreate the pageradapter
            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            pager.setAdapter(null);
            pager.setAdapter(new LoadingAdapter(getSupportFragmentManager()));

            //now start the adapter loading in a separate thread
            new PagerAdapterLoader().execute(this);

            //load the right last-update timestamp
            String lastUpdate = getString(R.string.last_update) + " " + getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getString(SharedPrefs.PREFIX_LAST_UPDATE + appMode + requestedVplanMode, "");
            NavigationView drawer = (NavigationView) findViewById(R.id.drawer_left);
            drawer.getMenu().findItem(R.id.lastUpdate).setTitle(lastUpdate);
        } //else just ignore the click
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        refreshInfoFab(position);
    }

    private void refreshInfoFab(int position) {

        //check if this vplan has announcements
        String announcements;
        datasource.open();

        Cursor c = datasource.query(SQLiteHelperVplan.TABLE_ANNOUNCEMENTS, new String[]{SQLiteHelperVplan.COLUMN_ANNOUNCEMENT});
        try {
            c.moveToPosition(position);
            announcements = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_ANNOUNCEMENT));

            datasource.close();

            //if there are announcements activate the info fab
            FloatingActionButton infoFab = (FloatingActionButton) findViewById(R.id.infoFab);
            if (announcements.isEmpty()) infoFab.hide();
            else {
                infoFab.show();

                //when infoFab is clicked show the announcements in a dialog
                infoFab.setOnClickListener(new InfoFabManager(announcements));

                //display a single-shot tutorial information about announcements
                showcase = new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(infoFab))
                        .setContentTitle(getString(R.string.title_announcements))
                        .setContentText(getString(R.string.message_tut_announcements))
                        .setStyle(R.style.ShowcaseTheme)
                        .singleShot(Tutorial.SHOT_ANNOUNCEMENTS)
                        .hideOnTouchOutside()
                        .build();
                showcase.hideButton();
            }
        } catch (Exception e) {

            //probably there was no data for the requested vplan, so just hide the fab
            FloatingActionButton infoFab = (FloatingActionButton) findViewById(R.id.infoFab);
            infoFab.hide();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class InfoFabManager implements View.OnClickListener {

        private AlertDialog.Builder builder;

        public InfoFabManager(String announcements) {

            //init the dialog immediately
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.announcements)
                    .setMessage(announcements);
        }

        @Override
        public void onClick(View v) {
            builder.show();
            if (showcase != null) showcase.hide();
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
            return new VplanPagerAdapter(getSupportFragmentManager(), getApplicationContext(), activities[0], mergedFilter);
        }

        @Override
        protected void onPostExecute(VplanPagerAdapter vplanPagerAdapter) {

            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            pager.setAdapter(null);
            pager.setAdapter(vplanPagerAdapter);
            vplanPagerAdapter.notifyDataSetChanged();

            //init the tab layout
            SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs);
            tabs.setCustomTabColorizer(position -> getColor(getResources(), R.color.blue, getTheme()));
            tabs.setDistributeEvenly(true);
            tabs.setViewPager(pager);

            pager.addOnPageChangeListener(MainActivity.this);

            //set a 1 dp margin between the fragments
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            pager.setPageMargin(Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));
            pager.setCurrentItem(getTodayVplanId(), false);

            refreshInfoFab(0);

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
            }
        }

        @Override
        protected Boolean doInBackground(Context... context) {
            return super.doInBackground(context);
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

                //if we are in tutorial mode, resume
                SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
                if (pref.getBoolean(SharedPrefs.TUT_RUNNING, false)) overflowTutorial();
            } else {
                stopProgressBar();

                resetRefreshAnimation();

                //notify about error
                Toast.makeText(getApplicationContext(), getString(R.string.download_error_title), Toast.LENGTH_SHORT).show();
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
                    Log.v("Wear Api", "DataMap: " + dataMap + "sent to: " + node.getDisplayName());
                else {
                    failCount++;
                    Log.e("Wear Api", "ERROR: failed to send DataMap! (" + failCount + ")");

                    //retry later
                    try {
                        if (failCount <= 3) {
                            Thread.sleep(2000);
                        } //else stop it after 3 times trying
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
