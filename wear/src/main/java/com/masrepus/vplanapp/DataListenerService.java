package com.masrepus.vplanapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;
import com.masrepus.vplanapp.constants.SharedPrefs;

/**
 * Created by samuel on 05.01.15.
 */
public class DataListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(Args.ACTION_UPDATE_UI)) {
            //start an update in the background
            sendBroadcast(new Intent(Args.ACTION_UPDATE_UI));
            Log.v("Wear Api", "Received message: " + messageEvent.getPath());
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {

            //check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                //check the data path and react accordingly
                String path = event.getDataItem().getUri().getPath();
                if (path.contentEquals(DataKeys.VPLAN)) {
                    Log.v("Wear Api", "Vplan data received on watch: " + dataMap);

                    saveVplanFiles(dataMap);
                } else if (path.contentEquals(DataKeys.HEADERS)) {
                    Log.v("Wear Api", "Headers received on watch: " + dataMap);

                    saveHeaders(dataMap);
                } else if (path.contentEquals(DataKeys.META_DATA)) {
                    Log.v("Wear Api", "Meta-data received on watch: " + dataMap);

                    saveLastUpdate(dataMap.getString(SharedPrefs.LAST_UPDATE));
                    saveTimePublishedTimestamps(dataMap.getStringArray(DataKeys.TIME_PUBLISHED_TIMESTAMPS));
                    saveCount(dataMap.getInt(DataKeys.DAYS));
                } else if (path.contentEquals(DataKeys.REQUEST)) {

                    if (dataMap.getString(DataKeys.ACTION).contentEquals(Args.ACTION_UPDATE_UI)) sendBroadcast(new Intent(this, MainActivity.class).setAction(Args.ACTION_UPDATE_UI));
                }
            } else Log.v("Wear Api", "Skipped incoming data item (type: " + event.getType() + ")");
        }
    }

    private void saveCount(int count) {

        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putInt(SharedPrefs.DAYS_COUNT, count);
        editor.apply();
    }

    private void saveTimePublishedTimestamps(String[] timePublishedTimestamps) {

        //save the timestamps to shared prefs according to their order in the array
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        int position = 0;
        for (String timePublished : timePublishedTimestamps) {

            editor.putString(SharedPrefs.TIME_PUBLISHED_PREFIX + String.valueOf(position), timePublished);
            editor.apply();
            position++;
        }
    }

    private void saveLastUpdate(String lastUpdate) {

        //save the last update timestamp to shared prefs
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(SharedPrefs.LAST_UPDATE, lastUpdate);
        editor.apply();
    }

    private void saveHeaders(DataMap dataMap) {

        //save the headers in shared prefs
        SharedPreferences pref = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        for (int i = 0; i < dataMap.size(); i++) {

            editor.putString(SharedPrefs.PREF_HEADER_PREFIX + String.valueOf(i), dataMap.getString(String.valueOf(i)));
            editor.apply();
        }

        Log.v("DataListenerService", "Successfully saved headers in SharedPrefs: " + dataMap);
    }

    private void saveVplanFiles(DataMap dataMap) {

        VPlanDataSource datasource = new VPlanDataSource(this);
        datasource.open();

        for (int i = 0; i < dataMap.size(); i++) {

            //clear existing data
            datasource.newTable(SQLiteHelperVplan.tablesVplan[i]);

            DataMap day = dataMap.getDataMap(String.valueOf(i));

            //iterate over the rows
            for (int r = 0; r < day.size(); r++) {

                Row row = new Row(day.getDataMap(String.valueOf(r)));

                //db insert
                datasource.createRowVplan(SQLiteHelperVplan.tablesVplan[i], row.getStunde(), row.getKlasse(), row.getStatus());
            }
        }

        datasource.close();

        Log.v("DataListenerService", "Data saved in db: " + dataMap.size() + " days");
    }
}
