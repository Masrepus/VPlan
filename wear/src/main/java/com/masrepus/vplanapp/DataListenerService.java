package com.masrepus.vplanapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.Keys;

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
                    Log.v(getPackageName(), "DataMap received on watch: " + dataMap);

                    saveReceivedData(dataMap);
                }
            } else Log.v(getPackageName(), "Skipped incoming data item (type: " + event.getType() + ")");
        }
    }

    private void saveReceivedData(DataMap dataMap) {

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
