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

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            //check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                //check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.contentEquals("/vplan")) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.v(getPackageName(), "Vplan data received on watch: " + dataMap);

                    saveVplanFiles(dataMap);
                } else if (path.contentEquals("/headers")) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.v(getPackageName(), "Headers received on watch: " + dataMap);

                    saveHeaders(dataMap);
                }
            } else Log.v(getPackageName(), "Skipped incoming data item (type: " + event.getType() + ")");
        }
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
