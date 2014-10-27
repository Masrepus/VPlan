package com.masrepus.vplanapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.Set;

public class DownloaderService extends Service {

    int vplanMode;
    int downloaded_total;
    String[] levels;
    int downloaded_levels = 0;
    private SharedPreferences.Editor editor;

    public DownloaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        editor = pref.edit();

        Set<String> levelsSet = settings.getStringSet(getString(R.string.pref_key_bg_upd_levels), null);

        if (levelsSet == null || levelsSet.size() == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        levels = levelsSet.toArray(new String[levelsSet.size()]);

        //execute for each level, but always wait until the last one is finished
        vplanMode = parseVplanMode(levels[0]);

        //now save the current vplan mode in shared prefs
        editor.putInt(MainActivity.PREF_VPLAN_MODE, vplanMode);
        editor.apply();

        new BgDownloader().execute(this);

        return START_NOT_STICKY;
    }

    public int parseVplanMode(String humanReadable) {

        if (humanReadable.contentEquals(getString(R.string.unterstufe))) return 1;
        else if (humanReadable.contentEquals(getString(R.string.mittelstufe))) return 2;
        else return 3;
    }

    private class BgDownloader extends AsyncDownloader {

        int notificationId;
        CancelReceiver cancelReceiver = new CancelReceiver();

        @Override
        protected int getRequestedVplanMode() {
            return vplanMode;
        }

        @Override
        protected void onProgressUpdate(Enum... progress) {

            String progressText = "";
            String bigTextSuffix = "";
            boolean error = false;
            switch ((MainActivity.ProgressCode) progress[0]) {

                case STARTED:
                    progressText = getString(R.string.preparing);
                    break;
                case PARSING_FINISHED:
                    progressText = String.valueOf(downloaded) + " von " + String.valueOf(total_downloads) + " " + getString(R.string.downloaded_prog);
                    break;
                case FINISHED_ALL:
                    downloaded_total += downloaded;
                    downloaded_levels++;
                    String files;
                    String level;
                    //handle singular or plural
                    if (downloaded_total > 1) files = getString(R.string.files);
                    else files = getString(R.string.file);

                    if (downloaded_levels > 1) level = getString(R.string.levels);
                    else level = getString(R.string.level);

                    progressText = getString(R.string.download_finished) + ": " + String.valueOf(downloaded_total) + " " + files;
                    bigTextSuffix = " (" + String.valueOf(downloaded_levels) + "/" + String.valueOf(levels.length) + " " + level + ")";

                    break;
                case ERR_NO_CREDS:
                case ERR_NO_INTERNET:
                case ERR_NO_INTERNET_OR_NO_CREDS:
                    progressText = getString(R.string.download_error_title);
                    error = true;
                    break;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sgplogob))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(progressText);
            if (progress[0] == MainActivity.ProgressCode.FINISHED_ALL) {

                //final text is a big one
                builder.setStyle(new NotificationCompat.BigTextStyle(builder).bigText(progressText + bigTextSuffix));
            }

            PendingIntent cancelPending = PendingIntent.getBroadcast(context, 0, new Intent("cancel download"), 0);
            PendingIntent retryPending = PendingIntent.getService(context, 0, new Intent(context, DownloaderService.class), 0);

            //if downloading hasn't started yet then display indeterminate
            if (progress[0] == MainActivity.ProgressCode.STARTED) {
                builder.setProgress(100, 0, true);
                builder.setOngoing(true);
                builder.addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPending);
                notificationId = 2;
            } else {
                //display a progressbar if still downloading
                if (downloaded == total_downloads) {
                    builder.setProgress(0, 0, false);
                    builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                    builder.setOngoing(false);
                    builder.setAutoCancel(true);
                    notificationId = 1;
                } else {
                    builder.setProgress(total_downloads, downloaded, false);
                    //if an error has happened then don't make the notification ongoing and add a retry action
                    if (!error) {
                        builder.setOngoing(true)
                                .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPending);
                        notificationId = 2;
                    } else {
                        builder.setProgress(0, 0, false)
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setOngoing(false)
                                .addAction(android.R.drawable.stat_notify_sync_noanim, getString(R.string.retry), retryPending)
                                .setAutoCancel(true);
                        notificationId = 1;
                    }
                }
            }

            context.registerReceiver(cancelReceiver, new IntentFilter("cancel download"));

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(resultPendingIntent);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            //when parsing is completed cancel the ongoing notification
            if (notificationId == 1) manager.cancel(2);
            manager.notify(notificationId, builder.build());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            context.unregisterReceiver(cancelReceiver);

            //only stop the service when all the levels are finished downloading
            if (downloaded_levels == levels.length) stopSelf();
            else {
                //parse the next vplan level
                vplanMode = parseVplanMode(levels[downloaded_levels]);

                //update shared prefs as well
                editor.putInt(MainActivity.PREF_VPLAN_MODE, vplanMode);
                editor.apply();

                new BgDownloader().execute(context);
            }
        }

        @Override
        protected void onCancelled(Boolean success) {
            context.unregisterReceiver(cancelReceiver);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
            stopSelf();
        }

        private class CancelReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                cancel(false);
            }
        }
    }
}
