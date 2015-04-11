package com.masrepus.vplanapp.communication;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;
import com.masrepus.vplanapp.constants.ProgressCode;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperVplan;
import com.masrepus.vplanapp.vplan.MainActivity;
import com.masrepus.vplanapp.vplan.Row;

import java.util.ArrayList;
import java.util.Set;

public class DownloaderService extends Service {

    int vplanMode;
    int downloaded_total;
    String[] levels;
    int downloaded_levels = 0;
    DataSource datasource;
    private SharedPreferences.Editor editor;
    private int lastRequestedVplanMode;
    private ArrayList<String> filterCurrent = new ArrayList<>();
    private GoogleApiClient apiClient;
    private boolean notifyWear;
    private boolean finishedSyncing = false;

    public DownloaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("DownloaderService", "Starting bg download service");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        editor = pref.edit();

        //check whether we must notify wear after completion
        String action = intent.getStringExtra(DataKeys.ACTION);
        if (action != null) {
            notifyWear = action.contentEquals(Args.NOTIFY_WEAR_UPDATE_UI);
            Log.d("Wear Api", "notifyWear = " + notifyWear);
        } else notifyWear = false;

        lastRequestedVplanMode = pref.getInt(SharedPrefs.VPLAN_MODE, VplanModes.UINFO);

        //get the current filter
        switch (lastRequestedVplanMode) {
            case VplanModes.UINFO:
                filterCurrent.addAll(pref.getStringSet(getString(R.string.pref_key_filter_uinfo), null));
                break;
            case VplanModes.MINFO:
                filterCurrent.addAll(pref.getStringSet(getString(R.string.pref_key_filter_minfo), null));
                break;
            case VplanModes.OINFO:
                filterCurrent.addAll(pref.getStringSet(getString(R.string.pref_key_filter_oinfo), null));
                break;
        }

        Set<String> levelsSet = settings.getStringSet(getString(R.string.pref_key_bg_upd_levels), null);

        if (levelsSet == null || levelsSet.size() == 0) {
            sendDataToWatch();
            stopSelf();
            return START_NOT_STICKY;
        }

        levels = levelsSet.toArray(new String[levelsSet.size()]);

        //execute for each level, but always wait until the last one is finished
        vplanMode = parseVplanMode(levels[0]);

        //now save the current vplan mode in shared prefs
        editor.putInt(SharedPrefs.VPLAN_MODE, vplanMode);
        editor.apply();

        new BgDownloader().execute(this);

        return START_NOT_STICKY;
    }

    public int parseVplanMode(String humanReadable) {

        if (humanReadable.contentEquals(getString(R.string.unterstufe))) return 1;
        else if (humanReadable.contentEquals(getString(R.string.mittelstufe))) return 2;
        else return 3;
    }

    private void sendDataToWatch() {

        DataMap dataMap = new DataMap();
        datasource = new DataSource(this);

        //init the api client
        buildApiClient();

        //get the number of available days
        datasource.open();
        Cursor c = datasource.query(SQLiteHelperVplan.TABLE_LINKS, new String[]{SQLiteHelperVplan.COLUMN_ID});
        int count = c.getCount();

        for (int i = 0; i < count; i++) {
            dataMap.putDataMap(String.valueOf(i), fillDataMap(i));
        }

        new SendToDataLayerThread(DataKeys.VPLAN, dataMap).execute();

        //now the headers
        dataMap = new DataMap();

        datasource.close();

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //get the header strings from shared prefs
        for (int i = 0; i < count; i++) {
            dataMap.putString(String.valueOf(i), pref.getString(SharedPrefs.PREFIX_VPLAN_CURR_DATE + String.valueOf(lastRequestedVplanMode) + String.valueOf(i), ""));
        }

        new SendToDataLayerThread(DataKeys.HEADERS, dataMap).execute();

        //send last updated timestamp and time published timestamps
        dataMap = new DataMap();

        String lastUpdate = pref.getString(SharedPrefs.PREFIX_LAST_UPDATE + AppModes.VPLAN + lastRequestedVplanMode, "");
        String[] timePublishedTimestamps = new String[count];

        for (int i = 0; i < count; i++) {

            //get each time published timestamp
            timePublishedTimestamps[i] = pref.getString(SharedPrefs.PREFIX_VPLAN_TIME_PUBLISHED + lastRequestedVplanMode + i, "");
        }

        dataMap.putString(SharedPrefs.PREFIX_LAST_UPDATE, lastUpdate);
        dataMap.putStringArray(DataKeys.TIME_PUBLISHED_TIMESTAMPS, timePublishedTimestamps);
        dataMap.putInt(DataKeys.DAYS, count);

        //check if we must notify wear that the update is finished
        if (notifyWear) new SendToDataLayerThread(DataKeys.META_DATA, dataMap, true).execute();
        else new SendToDataLayerThread(DataKeys.META_DATA, dataMap).execute();
    }

    private DataMap fillDataMap(int id) {

        DataMap dataMap = new DataMap();

        //query the data for the right vplan -> get requested table name by passed arg
        String tableName = SQLiteHelperVplan.tablesVplan[id];

        if (datasource.hasData(tableName)) {

            Cursor c = datasource.query(tableName, new String[]{SQLiteHelperVplan.COLUMN_ID, SQLiteHelperVplan.COLUMN_GRADE, SQLiteHelperVplan.COLUMN_STUNDE,
                    SQLiteHelperVplan.COLUMN_STATUS});

            ArrayList<Row> tempList = new ArrayList<>();

            //check whether filter is active
            Boolean isFilterActive = getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false);

            //if filter is active, then use it after filling the Arraylist
            if (isFilterActive) {

                while (c.moveToNext()) {
                    Row row = new Row();

                    //only add to list if row isn't null
                    String help = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE));
                    if (help.contentEquals("Klasse")) continue;
                    if (help.contentEquals("")) continue;

                    row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
                    row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));
                    row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));

                    tempList.add(row);
                }

                //now perform the filtering
                int position = 0;

                for (Row currRow : tempList) {
                    String klasse = currRow.getKlasse();
                    boolean isNeeded = false;

                    //look whether this row's klasse attribute contains any of the classes to filter for
                    if (filterCurrent.size() > 0) {
                        for (int i = 0; i <= filterCurrent.size() - 1; i++) {
                            char[] klasseFilter = filterCurrent.get(i).toCharArray();

                            //check whether this is oinfo, as in this case, the exact order of the filter chars must be given as well
                            if (lastRequestedVplanMode == VplanModes.OINFO) {
                                String filterItem = filterCurrent.get(i);
                                isNeeded = klasse.contentEquals("Q" + filterItem);

                                if (isNeeded) break;
                                if (klasse.contentEquals("")) isNeeded = true;
                            } else { //in u/minfo the order doesn't play a role

                                //if klasse contains all of the characters of the filter string, isNeeded will be true, because if one character returns false, the loop is stopped
                                for (int y = 0; y <= klasseFilter.length - 1; y++) {
                                    if (klasse.contains(String.valueOf(klasseFilter[y]))) {
                                        isNeeded = true;
                                    } else {
                                        isNeeded = false;
                                        break;
                                    }
                                }
                                if (isNeeded) break;

                                //also set isneeded to true if klasse=""
                                if (klasse.contentEquals("")) isNeeded = true;
                            }
                        }
                    } else {
                        //if there is no item in the filter list, then still take the rows without a value for class
                        isNeeded = klasse.contentEquals("");
                    }
                    //if the test was positive, then add the current Row to the map
                    if (isNeeded) {
                        dataMap.putDataMap(String.valueOf(position), currRow.putToDataMap(new DataMap()));
                        position++;
                    }
                }

            } else {
                // just fill the list normally
                int position = 0;
                while (c.moveToNext()) {
                    Row row = new Row();

                    //only add to list if row isn't null
                    String help = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE));
                    if (help.contentEquals("Klasse")) continue;
                    if (help.contentEquals("")) continue;

                    row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
                    row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));
                    row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));

                    dataMap.putDataMap(String.valueOf(position), row.putToDataMap(new DataMap()));
                    position++;
                }
            }
        }

        return dataMap;
    }

    private void buildApiClient() {

        apiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("Google Services", "onConnectionFailed: " + connectionResult);
                    }
                })

                        //request access to the Wearable API
                .addApi(Wearable.API)
                .build();
        apiClient.connect();
    }

    private class SendToDataLayerThread extends AsyncTask<Void, Void, Void> {

        private boolean notifyOnFinish;
        private String path;
        private DataMap dataMap;
        private int failCount = 0;
        private boolean message;

        //Constructor for sending data objects to the data layer
        public SendToDataLayerThread(String path, DataMap dataMap) {
            this.path = path;
            this.dataMap = dataMap;
            message = false;
        }

        //Constructor used when calling the last data upload
        public SendToDataLayerThread(String path, DataMap dataMap, boolean notifyOnFinish) {
            this.path = path;
            this.dataMap = dataMap;
            message = false;
            this.notifyOnFinish = notifyOnFinish;
        }

        //Constructor for messages
        public SendToDataLayerThread(String path) {
            this.path = path;
            message = true;
        }

        private void sendData() {

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
            for (Node node : nodes.getNodes()) {

                //Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);

                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(apiClient, request).await();

                if (result.getStatus().isSuccess())
                    Log.v("Wear Api", path + " " + dataMap + "sent to: " + node.getDisplayName());
                else {
                    failCount++;
                    Log.e("Wear Api", "ERROR: failed to send DataMap! (" + failCount + ")");

                    //retry later
                    try {
                        if (failCount <= 3) {
                            Thread.sleep(2000);
                        } //else stop it after 3 times trying
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        private void sendMessage() {

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();

            for (Node node : nodes.getNodes()) {

                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(apiClient, node.getId(), path, null).await();

                //check for success
                if (!result.getStatus().isSuccess())
                    Log.e("Wear Api", "ERROR: failed to send Message: " + result.getStatus());
                else Log.v("Wear Api", "Successfully sent message: " + path + " to " + node);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {

            //wait for connection
            if (!apiClient.isConnected()) try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (message) sendMessage();
            else sendData();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //notify wear if this wass the last syncing operation
            if (notifyOnFinish) new SendToDataLayerThread(Args.ACTION_UPDATE_UI).execute();
        }
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
            switch ((ProgressCode) progress[0]) {

                case STARTED:
                    progressText = getString(R.string.preparing);
                    break;
                case PARSING_FINISHED:
                    progressText = String.valueOf(downloaded_files) + " von " + String.valueOf(total_downloads) + " " + getString(R.string.downloaded_prog);
                    break;
                case FINISHED_ALL_DAYS:
                    //get the sum of all downloaded files
                    downloaded_total += downloaded_files;
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
                    //prevent a infinite download loop: don't try this class level again this time
                    downloaded_total += downloaded_files;
                    downloaded_levels++;
                    progressText = getString(R.string.download_error_title);
                    error = true;
                    break;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_v2))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(progressText);
            if (progress[0] == ProgressCode.FINISHED_ALL_DAYS) {

                //final text is a big one
                builder.setStyle(new NotificationCompat.BigTextStyle(builder).bigText(progressText + bigTextSuffix));
            }

            PendingIntent cancelPending = PendingIntent.getBroadcast(context, 0, new Intent("cancel download"), 0);
            PendingIntent retryPending = PendingIntent.getService(context, 0, new Intent(context, DownloaderService.class), 0);

            //if downloading hasn't started yet then display indeterminate
            if (progress[0] == ProgressCode.STARTED) {
                builder.setProgress(100, 0, true);
                builder.setOngoing(true);
                builder.addAction(R.drawable.ic_cancel, getString(R.string.cancel), cancelPending);
                notificationId = 2;
            } else {
                //display a progressbar if still downloading
                if (downloaded_files == total_downloads) {

                    //check if there was an error downloading
                    if (error) {
                        builder.setProgress(0, 0, false)
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setOngoing(false)
                                .addAction(android.R.drawable.stat_notify_sync_noanim, getString(R.string.retry), retryPending)
                                .setAutoCancel(true);
                        notificationId = 1;
                    } else {
                        builder.setProgress(0, 0, false);
                        builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                        builder.setOngoing(false);
                        builder.setAutoCancel(true);
                        notificationId = 1;
                    }
                } else {
                    builder.setProgress(total_downloads, downloaded_files, false);
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

            //only stop the service when all the levels are finished downloading or if there was an error
            if (!success) {
                stopSelf();
                return;
            }

            if (downloaded_levels == levels.length) {

                //sync to wear
                sendDataToWatch();

                stopSelf();
            } else {
                //parse the next vplan level
                vplanMode = parseVplanMode(levels[downloaded_levels]);

                //update shared prefs as well
                editor.putInt(SharedPrefs.VPLAN_MODE, vplanMode);
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
                cancel(true);
            }
        }
    }
}
