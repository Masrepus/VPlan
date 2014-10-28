package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Created by samuel on 01.09.14.
 */
public class SettingsActivity extends ActionBarActivity {


    /**
     * Called when the Activity is first created.
     * Takes care of displaying the settings content as a SettingsFragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

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
