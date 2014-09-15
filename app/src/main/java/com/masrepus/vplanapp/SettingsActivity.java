package com.masrepus.vplanapp;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by samuel on 01.09.14.
 */
public class SettingsActivity extends Activity {


    /**
     * Called when the Activity is first created.
     * Takes care of displaying the settings content as a SettingsFragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //display settings fragment as main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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
