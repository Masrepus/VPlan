package com.masrepus.vplanapp;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.SharedPrefs;

/**
 * Created by samuel on 05.01.15.
 */
public class DataListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {

            //check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                //check the data path and react accordingly
                String path = event.getDataItem().getUri().getPath();
                if (path.contentEquals("/vplan")) {
                    Log.v(getPackageName(), "Vplan data received on watch: " + dataMap);

                    saveVplanFiles(dataMap);
                } else if (path.contentEquals("/headers")) {
                    Log.v(getPackageName(), "Headers received on watch: " + dataMap);

                    saveHeaders(dataMap);
                } else if (path.contentEquals("/timestamps")) {
                    Log.v(getPackageName(), "Timestamps received on watch: " + dataMap);

                    saveLastUpdate(dataMap.getString("lastUpdate"));
                    saveTimePublishedTimestamps(dataMap.getStringArray("timePublishedTimestamps"));
                }
            } else Log.v(getPackageName(), "Skipped incoming data item (type: " + event.getType() + ")");
        }
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

        Log.v(getPackageName(), "Successfully saved headers in SharedPrefs: " + dataMap);
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

        Log.v(getPackageName(), "Data saved in db: " + dataMap.size() + " days");
    }
}
