package com.masrepus.vplanapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by samuel on 05.10.14.
 */
public class BootReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        //set the alarm
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        refreshBgUpdates(pref.getBoolean(context.getString(R.string.pref_key_bg_updates), false), Integer.valueOf(pref.getString(context.getString(R.string.pref_key_upd_int), "")));
    }

    private void refreshBgUpdates(boolean activated, int interval) {

        //get the download intent from downloadservice and use it for alarmmanager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent downloadIntent = new Intent(context, DownloaderService.class);

        PendingIntent pendingDownloadIntent = PendingIntent.getService(context, 0, downloadIntent, 0);

        if (activated) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval * AlarmManager.INTERVAL_HOUR, interval * AlarmManager.INTERVAL_HOUR, pendingDownloadIntent);
            Log.d(context.getPackageName(), "successfully set alarm for auto update");
        }
    }
}
