package com.masrepus.vplanapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.SharedPrefs;


public class TimetableActivity extends ActionBarActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timetable_activity);

        //activate the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        prepareDrawer();
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
        }

        return super.onOptionsItemSelected(item);
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
}
