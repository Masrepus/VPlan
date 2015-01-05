package com.masrepus.vplanapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

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

                    //broadcast the data
                    sendBroadcast(new Intent("data received").putExtra("data", dataMap.toBundle()));
                }
            }
        }
    }
}
