package com.masrepus.vplanapp.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.masrepus.vplanapp.R;

/**
 * Created by samuel on 01.09.14.
 */
public class SettingsActivity extends ActionBarActivity {


    /**
     * Called when the Activity is first created.
     * Takes care of displaying the activity_settings content as a SettingsFragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //set actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    /**
     * By setting the result in onDestroy(), SettingsActivity notifies MainActivity of its closure
     */
    @Override
    protected void onDestroy() {
        setResult(0);
        super.onDestroy();
    }
}
