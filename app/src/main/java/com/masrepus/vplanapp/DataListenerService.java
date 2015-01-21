package com.masrepus.vplanapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;

/**
 * Created by samuel on 18.01.15.
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
                if (path.contentEquals(DataKeys.REQUEST)) {
                    Log.v(getPackageName(), "Request received on phone: " + dataMap);

                    if (dataMap.getString(DataKeys.ACTION).contentEquals(Args.ACTION_REFRESH)) {

                        //start an update in the background
                        startService(new Intent(getApplicationContext(), DownloaderService.class).putExtra(DataKeys.ACTION, Args.NOTIFY_WEAR_UPDATE_UI));
                    }
                }
            } else Log.v(getPackageName(), "Skipped incoming data item (type: " + event.getType() + ")");
        }
    }
}
