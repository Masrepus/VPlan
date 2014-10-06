package com.masrepus.vplanapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class DownloaderService extends Service {
    public DownloaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new BgDownloader().execute(this);

        return START_NOT_STICKY;
    }

    private class BgDownloader extends AsyncDownloader {

        int notificationId;
        CancelReceiver cancelReceiver = new CancelReceiver();

        @Override
        protected void onProgressUpdate(Enum... progress) {

            String progressText = "";
            boolean error = false;
            switch ((MainActivity.ProgressCode) progress[0]) {

                case STARTED:
                    progressText = getString(R.string.preparing);
                    break;
                case PARSING_FINISHED:
                    progressText = String.valueOf(downloaded) + " von " + String.valueOf(total_downloads) + " " + getString(R.string.downloaded_prog);
                    break;
                case FINISHED_ALL:
                    progressText = getString(R.string.download_finished) + ": " + String.valueOf(downloaded) + " " + getString(R.string.files);
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

            PendingIntent cancelPending = PendingIntent.getBroadcast(context, 0, new Intent("cancel download"), 0);
            PendingIntent retryPending = PendingIntent.getService(context, 0, new Intent(context, DownloaderService.class), 0);

            //if downloading hasn't started yet then display indeterminate
            if (progress[0] == MainActivity.ProgressCode.STARTED) {
                builder.setProgress(100, 0, true);
                builder.setOngoing(true);
                builder.addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPending);
            } else {
                //display a progressbar if still downloading
                if (downloaded == total_downloads) {
                    builder.setProgress(0, 0, false);
                    builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                    builder.setOngoing(false);
                } else {
                    builder.setProgress(total_downloads, downloaded, false);
                    //if an error has happened then don't make the notification ongoing and add a retry action
                    if (!error) {
                        builder.setOngoing(true)
                                .addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPending);
                    }
                    else {
                        builder.setProgress(0, 0, false)
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setOngoing(false)
                                .addAction(android.R.drawable.stat_notify_sync_noanim, getString(R.string.retry), retryPending);
                    }
                }
            }

            context.registerReceiver(cancelReceiver, new IntentFilter("cancel download"));

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(resultPendingIntent);

            notificationId = 001;

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            manager.notify(notificationId, builder.build());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            context.unregisterReceiver(cancelReceiver);
            stopSelf();
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
