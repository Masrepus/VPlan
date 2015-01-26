package com.masrepus.vplanapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;

/**
 * Created by samuel on 18.01.15.
 */
public class DataListenerService extends WearableListenerService {


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(Args.ACTION_REFRESH)) {
            //start an update in the background
            startService(new Intent(getApplicationContext(), DownloaderService.class).putExtra(DataKeys.ACTION, Args.NOTIFY_WEAR_UPDATE_UI));
            Log.v(getPackageName(), "Received message: " + messageEvent.getPath());
        }
    }
}