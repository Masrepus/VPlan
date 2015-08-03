package com.masrepus.vplanapp.communication;

import android.content.Intent;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.CrashlyticsKeys;
import com.masrepus.vplanapp.constants.DataKeys;

import io.fabric.sdk.android.Fabric;

/**
 * Created by samuel on 18.01.15.
 */
public class DataListenerService extends WearableListenerService {


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(Args.ACTION_REFRESH)) {
            //start an update in the background
            startService(new Intent(getApplicationContext(), DownloaderService.class).putExtra(DataKeys.ACTION, Args.NOTIFY_WEAR_UPDATE_UI));
            Log.v("Wear Api", "Received message: " + messageEvent.getPath());
        } else if (messageEvent.getPath().equals(Args.WEAR_APP_OPENED)) {

            //notify answers
            Fabric.with(this, new Crashlytics());
            Answers.getInstance().logCustom(new CustomEvent(CrashlyticsKeys.EVENT_WEAR_APP_OPENED));
        }
    }
}
