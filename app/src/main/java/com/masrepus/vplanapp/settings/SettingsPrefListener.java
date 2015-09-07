package com.masrepus.vplanapp.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.masrepus.vplanapp.network.DownloaderService;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by samuel on 05.12.14.
 */
public class SettingsPrefListener implements SharedPreferences.OnSharedPreferenceChangeListener, Serializable {

    private Map<String, ?> keys;
    private Context context;

    public SettingsPrefListener(Context context) {
        this.context = context;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        //activity_settings have been changed, so update the filter array if the classes to filter have been changed
        refreshFilters();
    }

    private void refreshFilters() {

        //create a new list and fill it
        ArrayList<String> filterUinfoTemp = new ArrayList<>();
        ArrayList<String> filterMinfoTemp = new ArrayList<>();
        ArrayList<String> filterOinfoTemp = new ArrayList<>();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        keys = pref.getAll();
        String[] uinfoKeys = {context.getString(R.string.key_grade5), context.getString(R.string.key_grade6), context.getString(R.string.key_grade7)};

        String[] minfoKeys = {context.getString(R.string.key_grade8), context.getString(R.string.key_grade9), context.getString(R.string.key_grade10)};

        //iterate through all shared prefs stringsets
        int mode;

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            //skip selected keys
            if (entry.getKey().contentEquals(context.getString(R.string.key_uname)) || entry.getKey().contentEquals(context.getString(R.string.key_pwd))
                    || entry.getKey().contentEquals(context.getString(R.string.pref_key_upd_int)) || entry.getKey().contentEquals(context.getString(R.string.pref_key_bg_upd_levels)))
                continue;

            //treat bg updates separately
            if (entry.getKey().contentEquals(context.getString(R.string.pref_key_bg_updates))) {

                int interval = Integer.valueOf(pref.getString(context.getString(R.string.pref_key_upd_int), ""));

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

        //save the filters in shared prefs
        pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        Set<String> unterstufeSet = new HashSet<>(filterUinfoTemp);
        Set<String> mittelstufeSet = new HashSet<>(filterMinfoTemp);
        Set<String> oberstufeSet = new HashSet<>(filterOinfoTemp);
        editor.putStringSet(context.getString(R.string.pref_key_filter_uinfo), unterstufeSet)
                .putStringSet(context.getString(R.string.pref_key_filter_minfo), mittelstufeSet)
                .putStringSet(context.getString(R.string.pref_key_filter_oinfo), oberstufeSet)
                .apply();
    }

    private void refreshBgUpdates(Boolean activated, int interval) {

        //find out whether we have to update the pending intent
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
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

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent downloadIntent = new Intent(context, DownloaderService.class);
        PendingIntent pendingDownloadIntent = PendingIntent.getService(context, 0, downloadIntent, flag);

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
}
