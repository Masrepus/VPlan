package com.masrepus.vplanapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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

        @Override
        protected void onProgressUpdate(Enum... progress) {

            String progressText = "";
            switch ((MainActivity.ProgressCode) progress[0]) {

                case STARTED:
                    progressText = getString(R.string.preparing);
                    break;
                case PARSING_FINISHED:
                    progressText = String.valueOf(downloaded) + " von " + String.valueOf(total_downloads) + getString(R.string.downloaded_prog);
                    break;
                case FINISHED_ALL:
                    progressText = getString(R.string.download_finished) + ": " + String.valueOf(downloaded) + " " + getString(R.string.files);
                    break;
                case ERR_NO_CREDS:
                case ERR_NO_INTERNET:
                case ERR_NO_INTERNET_OR_NO_CREDS:
                    progressText = getString(R.string.download_error_title);
                    break;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sgplogob))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(progressText);

            //if downloading hasn't started yet then display indeterminate
            if (progress[0] == MainActivity.ProgressCode.STARTED) {
                builder.setProgress(100, 0, true);
                builder.setOngoing(true);
            } else {
                //display a progressbar if still downloading
                if (downloaded == total_downloads) {
                    builder.setProgress(0, 0, false);
                    builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                    builder.setOngoing(false);
                } else {
                    builder.setProgress(total_downloads, downloaded, false);
                    builder.setOngoing(true);
                }
            }

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(resultPendingIntent);

            notificationId = 001;

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            manager.notify(notificationId, builder.build());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            stopSelf();
        }
    }
}
