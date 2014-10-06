package com.masrepus.vplanapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * Created by samuel on 05.10.14.
 */
public class BootReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        //set the alarm
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);

        refreshBgUpdates(Boolean.valueOf(pref.getString(context.getString(R.string.pref_key_bg_updates), "false")), Integer.valueOf(pref.getString(context.getString(R.string.pref_key_upd_int), "")));
    }

    private void refreshBgUpdates(Boolean activated, int interval) {

        //get the download intent from downloadservice and use it for alarmmanager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent downloadIntent = new Intent(context, DownloaderService.class);

        //find out whether we have to update the pending intent
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        Boolean wasActiveBefore = pref.getBoolean(MainActivity.PREF_IS_BG_UPD_ACTIVE, false);
        int flag = 0;
        if (wasActiveBefore) flag = PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingDownloadIntent = PendingIntent.getService(context, 0, downloadIntent, flag);

        if (activated) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval * AlarmManager.INTERVAL_HOUR, interval * AlarmManager.INTERVAL_HOUR, pendingDownloadIntent);
        } else alarmManager.cancel(pendingDownloadIntent);
    }
}
