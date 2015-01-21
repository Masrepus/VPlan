package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WearableListView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.DataKeys;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity implements View.OnClickListener {

    private int todayVplan;
    private GoogleApiClient apiClient;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectApiClient();

        initTodayVplan();

        //create a new pager adapter and assign it to the pager
        VplanPagerAdapter adapter = new VplanPagerAdapter(getFragmentManager(), this);
        GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        //set a 1 dp margin between the fragments
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        pager.setPageMargins(0, Math.round(1 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)));

        //scroll to today's vplan
        pager.setCurrentItem(1, todayVplan);
    }

    private void initTodayVplan() {

        SharedPreferences prefs = getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //save the position of today's vplan in shared prefs
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("HH");

        for (int i = 0; i < 5; i++) {

            String title = prefs.getString(SharedPrefs.PREF_HEADER_PREFIX + String.valueOf(i), "");
            //skip to the next one if this header doesn't exist
            if (title.contentEquals("")) continue;

            if ((String.valueOf(title)).contains(format.format(calendar.getTime())) && Integer.valueOf(hour.format(calendar.getTime())) < 17) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(SharedPrefs.TODAY_VPLAN, i);
                editor.apply();
                todayVplan = i;
            } else if (Integer.valueOf(hour.format(calendar.getTime())) >= 17) {
                calendar = Calendar.getInstance();
                if (calendar.get(Calendar.DAY_OF_WEEK) >= Calendar.FRIDAY) {
                    int daysToMonday = (Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 2) % 7;
                    calendar.add(Calendar.DAY_OF_MONTH, daysToMonday);
                } else calendar.add(Calendar.DAY_OF_MONTH, 1);
                if ((String.valueOf(title)).contains(format.format(calendar.getTime()))) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(SharedPrefs.TODAY_VPLAN, i);
                    editor.apply();
                    todayVplan = i;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        TextView noItemsTV = (TextView) findViewById(R.id.noItemsTV);
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);

        //update the ui
        noItemsTV.setText(getString(R.string.refreshing));
        progress.setVisibility(View.VISIBLE);

        //fire the refresh request
        DataMap dataMap = new DataMap();
        dataMap.putString(DataKeys.ACTION, Args.ACTION_REFRESH);

        new SendToDataLayerThread(DataKeys.REQUEST, dataMap).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //now register a broadcastreceiver that waits for the phone's answer that update is done
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //notify the pager adapter
                GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
                pager.getAdapter().notifyDataSetChanged();
            }
        };

        registerReceiver(receiver, new IntentFilter(Args.ACTION_UPDATE_UI));
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    private void connectApiClient() {

        apiClient = new GoogleApiClient.Builder(this)
                //request access to the Wearable API
                .addApi(Wearable.API)
                .build();
        apiClient.connect();
    }

    private class SendToDataLayerThread extends Thread {

        private String path;
        private DataMap dataMap;
        private int failCount = 0;

        //Constructor for sending data objects to the data layer
        public SendToDataLayerThread(String path, DataMap dataMap) {
            this.path = path;
            this.dataMap = dataMap;
        }

        @Override
        public void run() {

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
            for (Node node : nodes.getNodes()) {

                //Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);

                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(apiClient, request).await();

                if (result.getStatus().isSuccess())
                    Log.v(getPackageName(), "DataMap: " + dataMap + "sent to: " + node.getDisplayName());
                else {
                    failCount++;
                    Log.e(getPackageName(), "ERROR: failed to send DataMap! (" + failCount + ")");

                    //retry later
                    try {
                        if (failCount <= 3) {
                            Thread.sleep(2000);
                        } //else stop it after 3 times trying
                    } catch (InterruptedException e) {}
                }
            }
        }
    }
}
