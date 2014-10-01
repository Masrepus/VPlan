package com.masrepus.vplanapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.ProgressBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DownloadService extends IntentService {

    private static final String ACTION_DOWNLOAD = "com.masrepus.vplanapp.action.DOWNLOAD";

    private static final String PARAM_UPDATE_UI = "com.masrepus.vplanapp.extra.UPDATE_UI";

    public static final String ACTION_BROADCAST_FINISHED = "com.masrepus.vplanapp.action.FINISHED";

    MainActivity.ProgressCode progress;
    int downloaded;
    int total_downloads;
    ProgressBar progressBar;
    Context context = getApplicationContext();
    VPlanDataSource datasource = new VPlanDataSource(getApplicationContext());

    /**
     * Starts this service to perform action download with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startDownload(Context context, Boolean flagUpdateUi) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(PARAM_UPDATE_UI, flagUpdateUi);
        context.startService(intent);
    }

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                final Boolean flagUpdateUi = intent.getBooleanExtra(PARAM_UPDATE_UI, false);
                handleActionDownload(flagUpdateUi);
            }
        }
    }

    /**
     * Handle action Download in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDownload(Boolean flagUpdateUi) {

    }

}