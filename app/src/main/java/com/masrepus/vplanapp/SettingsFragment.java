package com.masrepus.vplanapp;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by samuel on 01.09.14.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load the preferences from xml
        addPreferencesFromResource(R.xml.settings);
    }
}
