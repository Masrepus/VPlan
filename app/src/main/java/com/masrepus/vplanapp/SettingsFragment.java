package com.masrepus.vplanapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

import java.util.Set;

/**
 * Created by samuel on 01.09.14.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load the preferences from xml
        addPreferencesFromResource(R.xml.settings);

        preferences = getPreferenceScreen().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);

        initSummary(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();

        //set up a listener whenever a key changes
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //update the summaries
        initSummary(getPreferenceScreen());
    }

    public void initSummary(Preference p) {

        //if p is a preferencegroup, run initSummary for its children
        if (p instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup) p;
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                initSummary(group.getPreference(i));
            }
        } else {
            updateSummary(p);
        }
    }

    private void updateSummary(Preference p) {
        if (p instanceof MultiSelectListPreference) {
            MultiSelectListPreference multiPref = (MultiSelectListPreference) p;

            //get the values that were saved in this preference item and join them with commas
            //treat bg update levels separately
            Set<String> values = preferences.getStringSet(multiPref.getKey(), null);

            StringBuilder builder = new StringBuilder();

            if (values != null) {
                for (String v : values) {
                    if (builder.length() > 0) builder.append(',').append(" ");
                    builder.append(v);
                }

                multiPref.setSummary(builder.toString());
            }
        }

        if (p instanceof EditTextPreference) {

            EditTextPreference etPref = (EditTextPreference) p;

            if (etPref.getKey().contentEquals(getString(R.string.key_uname))) {

                //it isn't the password field
                etPref.setSummary(preferences.getString(etPref.getKey(), ""));
            } else etPref.setSummary("*****");
        }

        if (p instanceof ListPreference) {

            ListPreference lPref = (ListPreference) p;

            lPref.setSummary(lPref.getEntry());
        }
    }
}
