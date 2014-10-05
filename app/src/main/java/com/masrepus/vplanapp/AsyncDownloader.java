package com.masrepus.vplanapp;

/**
 * Created by samuel on 30.09.14.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

/**
 * The async task used for background-parsing of online data
 */
abstract class AsyncDownloader extends AsyncTask<Context, Enum, Boolean> {

    private static final int BASIC = MainActivity.BASIC;
    private static final int UINFO = MainActivity.UINFO;
    private static final int MINFO = MainActivity.MINFO;
    private static final int OINFO = MainActivity.OINFO;
    MainActivity.ProgressCode progress;
    int downloaded;
    int total_downloads;
    Context context;
    ProgressBar progressBar;
    VPlanDataSource datasource;
    private int requestedVplanId;
    private String currentVPlanLink;
    private String timePublished;
    private int requestedVplanMode;

    /**
     * Starts the process of parsing
     *
     * @param context Used for method calls that require a context parameter
     * @return returns true if everything went well
     */
    @Override
    final protected Boolean doInBackground(Context... context) {

        this.context = context[0];
        datasource = new VPlanDataSource(this.context);

        SharedPreferences pref = this.context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();

        requestedVplanMode = pref.getInt(MainActivity.PREF_VPLAN_MODE, UINFO);

        //download new data and then refresh pager adapter

        try {
            publishProgress(MainActivity.ProgressCode.STARTED);
            updateAvailableFilesList();
        } catch (Exception e) {
            //check whether this is because of missing creds
            if (e.getMessage() == "no creds available") {
                publishProgress(MainActivity.ProgressCode.ERR_NO_CREDS);
                return false;
            } else if (e.getMessage().contentEquals("failed to connect")) {
                publishProgress(MainActivity.ProgressCode.ERR_NO_INTERNET_OR_NO_CREDS);
                return false;
            } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                publishProgress(MainActivity.ProgressCode.ERR_NO_INTERNET);
                return false;
            }
        }

        datasource.open();

        Cursor c = datasource.query(MySQLiteHelper.TABLE_LINKS, new String[]{MySQLiteHelper.COLUMN_URL});

        try {
            total_downloads = c.getCount();
            downloaded = 0;

            while (c.moveToNext()) {
                //load every available vplan into the db
                requestedVplanId = c.getPosition();
                currentVPlanLink = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_URL));

                editor.putInt(MainActivity.PREF_REQUESTED_VPLAN_ID, requestedVplanId)
                        .putString(MainActivity.PREF_CURR_VPLAN_LINK, currentVPlanLink);
                editor.apply();

                parseDataToSql();

                downloaded = c.getPosition() + 1;
                publishProgress(MainActivity.ProgressCode.PARSING_FINISHED);
            }

            publishProgress(MainActivity.ProgressCode.FINISHED_ALL);

            return true;
        } catch (Exception e) {

            //check whether this is because of missing creds
            if (e.getMessage() == "no creds available") {
                publishProgress(MainActivity.ProgressCode.ERR_NO_CREDS);
            } else if (e.getMessage().contentEquals("failed to connect")) {
                publishProgress(MainActivity.ProgressCode.ERR_NO_INTERNET_OR_NO_CREDS);
            } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                publishProgress(MainActivity.ProgressCode.ERR_NO_INTERNET);
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) refreshLastUpdate();
    }

    /**
     * Takes care of all the downloading and db-inserting
     *
     * @throws Exception is thrown only if the returned encoding from encodeCredentials() was null or if therer has been an error while downloading
     */
    public void parseDataToSql() throws Exception {

        String encoding = encodeCredentials();

        if (encoding == null && requestedVplanMode != MainActivity.OINFO)
            throw new Exception("no creds available");

        Document doc;

        //load vplan xhtml file and select the right table into Elements table
        try {
            doc = Jsoup.connect(findRequestedVPlan()).header("Authorization", "Basic " + encoding).post();
        } catch (Exception e) {
            if (encoding == null) {
                if (requestedVplanMode != MainActivity.OINFO)
                    throw new Exception("failed to connect without creds");
                else throw new Exception("failed to connect oinfo");
            } else throw new Exception("failed to connect");
        }

        //tempContent contains all found tables
        Elements tempContent = doc.select("table[class=hyphenate]");
        //the desired table is second child of tempContent
        //each child of tableRows is one table row:
        Element headerCurrentDate;
        Elements tableRows;
        Elements availableFiles;

        try {
            tableRows = tempContent.get(1).child(0).children();
        } catch (Exception e) {
            tableRows = null;
        }

        //put the text of the h2 element after availableFiles table into headerCurrentDate
        try {
            headerCurrentDate = doc.select("h2").first();
        } catch (Exception e) {
            headerCurrentDate = null;
        }


        //get timePublished timestamp
        try {
            timePublished = doc.select("p").first().text();
        } catch (Exception e) {
            timePublished = "";
        }

        saveCurrentTimestamp(headerCurrentDate);

        //put the contents of the first table (available files) into another elements object for further processing
        try {
            availableFiles = tempContent.get(0).children().first().children();
        } catch (Exception e) {
            availableFiles = null;
        }

        parseVplan(tableRows);

        parseAvailableFiles(availableFiles);
    }

    public void insertVplanRow(String stunde, String klasse, String status, int position) {

        //sql insert of all three columns, but only if they aren't all empty
        if (stunde != null && !klasse.contentEquals("Klasse")) {

            switch (requestedVplanId) {
                case 0:
                    datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_0, position, stunde, klasse, status);
                    break;
                case 1:
                    datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_1, position, stunde, klasse, status);
                    break;
                case 2:
                    datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_2, position, stunde, klasse, status);
                    break;
                case 3:
                    datasource.newTable(MySQLiteHelper.TABLE_VPLAN_3);
                    datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_3, position, stunde, klasse, status);
                    break;
                case 4:
                    datasource.createRowVplan(MySQLiteHelper.TABLE_VPLAN_4, position, stunde, klasse, status);
                    break;
            }

        }
    }

    public void clearExistingTable() {

        //clear the existing table for the requested vplan
        switch (requestedVplanId) {
            case 0:
                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_0);
                break;
            case 1:
                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_1);
                break;
            case 2:
                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_2);
                break;
            case 3:
                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_3);
                break;
            case 4:
                datasource.newTable(MySQLiteHelper.TABLE_VPLAN_4);
                break;
        }
    }

    public void saveCurrentTimestamp(Element headerCurrentDate) {

        //only take the current day of the week and the date out of the header text; delete the space before the split string
        String[] separated = null;
        if (headerCurrentDate != null) {
            separated = headerCurrentDate.text().split("für");
        }
        String currentDate = null;
        if (separated != null) {
            if (separated.length <= 2) currentDate = separated[1].trim();
            else currentDate = separated[2].trim();
        }

        //now save the current loaded vplan's date and its last-changed timestamp for later usage
        SharedPreferences pref = this.context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(MainActivity.PREF_PREFIX_VPLAN_CURR_DATE + String.valueOf(requestedVplanId), currentDate);
        editor.putString(MainActivity.PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanId), timePublished);
        editor.apply();
    }

    public void parseVplan(Elements tableRows) {

        if (tableRows != null) {
            int position = 0;

            datasource.open();

            clearExistingTable();

            for (Element row : tableRows) {


                String[] columns = new String[row.children().size()];
                String stunde = null;
                String klasse = null;
                String status = null;

                //distribute the row's content into an array in order to get the columns
                for (int c = 0; c + 1 <= row.children().size(); c++) {
                    columns[c] = row.child(c).text();
                }

                //put the data into the right strings, all strings from columns >= column 3 into status column
                if (columns.length > 1) {
                    klasse = columns[0];
                    stunde = columns[1];

                    if (columns.length > 2) {
                        //insert the third column into status, then add all the remaining columns to it as well
                        status = columns[2];
                    } else status = "";
                }

                for (int i = 3; i + 1 <= row.children().size(); i++) {
                    status += " ";
                    status += columns[i];
                }

                insertVplanRow(stunde, klasse, status, position);

                position++;
            }
            datasource.close();
        }
    }

    public void parseAvailableFiles(Elements availableFiles) {

        if (availableFiles != null) {

            int position = 0;
            datasource.open();
            datasource.newTable(MySQLiteHelper.TABLE_LINKS);

            //now distribute the contents of availableFiles into a new list for the selection spinner
            for (Element row : availableFiles) {
                String url;
                String tag;

                tag = row.child(0).text();
                url = row.child(0).child(0).attributes().get("href");

                //sql insert
                datasource.createRowLinks(position, tag, url);

                position++;
            }

            datasource.close();
        }
    }

    /**
     * Called in order to find the correct url for the currently requested vplan mode. To accomplish that, it passes the request on to getVPlanUrl with the current mode
     *
     * @return the url as a String
     */
    private String findRequestedVPlan() {

        //return the appropriate vplan for the requested mode, if it hasn't been initialised for any reason, return the uinfo url
        String url = "";

        switch (requestedVplanMode) {

            case MainActivity.UINFO:
                url = getVPlanUrl(MainActivity.UINFO, false);
                break;
            case MainActivity.MINFO:
                url = getVPlanUrl(MainActivity.MINFO, false);
                break;
            case MainActivity.OINFO:
                url = getVPlanUrl(MainActivity.OINFO, false);
                break;
        }

        if (url != "") return url;
        else return getVPlanUrl(MainActivity.UINFO, false);
    }

    /**
     * Called by findRequestedVplan in order to find the correct vplan url
     *
     * @param version            either UINFO, MINFO or OINFO, depending on the current mode
     * @param includeCredentials only true if the url will be used to open the website in a browser, which appends username:password@ before the url
     * @return the url as a String
     */
    private String getVPlanUrl(int version, boolean includeCredentials) {

        // depending on the version requested return the appropriate string, optionally with credentials inside header
        String vplanBase;

        if (includeCredentials) {

            //uname and pwd are stored in settings sharedprefs
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String uname = pref.getString(this.context.getString(R.string.key_uname), "");
            String pwd = pref.getString(this.context.getString(R.string.key_pwd), "");

            vplanBase = "http://" + uname + ":" + pwd + "@" + this.context.getString(R.string.vplan_base_cred);
        } else {
            vplanBase = this.context.getString(R.string.vplan_base_url);
        }

        String url = "";
        switch (version) {
            case BASIC:
                return vplanBase;
            case UINFO:
                url = vplanBase + "pw/" + "urekursiv.php";
                break;
            case MINFO:
                url = vplanBase + "pw/" + "mrekursiv.php";
                break;
            case OINFO:
                url = vplanBase + "oinfo/" + "srekursiv.php";
                break;
        }

        if (currentVPlanLink != null && !currentVPlanLink.contentEquals("")) {
            String date = currentVPlanLink.split("_")[2];

            return url + "?datei=schuelerplan_vom_" + date;
        } else return url;
    }

    /**
     * Encodes the saved username and password with Base64 encoder
     *
     * @return the credentials as single, encoded String; null if there was any error
     */
    public String encodeCredentials() {
        //encode uname and pw for http post
        //uname and pwd are stored in settings sharedPrefs
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String uname = pref.getString(context.getString(R.string.key_uname), "");
        String pwd = pref.getString(context.getString(R.string.key_pwd), "");

        if (uname.contentEquals("") || pwd.contentEquals("")) return null;

        String creds = uname + ":" + pwd;

        byte[] data = null;
        try {
            data = creds.getBytes("UTF-8");
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            Log.e(context.getPackageName(), context.getString(R.string.err_getBytes));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(context.getPackageName(), context.getString(R.string.err_getBytes));
        }

        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    /**
     * Checks whether the device is online and if this is true it fetches the currently available files from the right internet page by calling findRequestedVplan()
     */
    private void updateAvailableFilesList() throws Exception {

        //load the list of files that are available just now, if internet connection is available, else just skip that
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {

            //we are online

            //encode uname and pw for http post
            String encoding = encodeCredentials();

            if (encoding == null && requestedVplanMode != OINFO)
                throw new Exception("failed to connect without creds");

            Document doc = null;
            try {
                doc = Jsoup.connect(findRequestedVPlan()).header("Authorization", "Basic " + encoding).post();
            } catch (IOException e) {
                e.printStackTrace();
                if (encoding == null) throw new Exception("failed to connect oinfo");
                else throw new Exception("failed to connect");
            }

            if (doc != null) {

                //tempContent contains all found tables
                Elements tempContent = doc.select("table[class=hyphenate]");
                //first table is the wanted one for available files list
                Elements availableFiles;

                try {
                    availableFiles = tempContent.get(0).child(0).children();
                } catch (Exception e) {
                    availableFiles = null;
                }

                //db input of available files + url's
                if (availableFiles != null) {

                    int position = 0;
                    datasource.open();
                    datasource.newTable(MySQLiteHelper.TABLE_LINKS);

                    //now distribute the contents of availableFiles into a new list for the selection spinner
                    for (Element row : availableFiles) {
                        String url;
                        String tag;

                        tag = row.child(0).text();
                        url = row.child(0).child(0).attributes().get("href");

                        //sql insert
                        datasource.createRowLinks(position, tag, url);

                        position++;
                    }

                    datasource.close();
                }
            }
        }
    }

    public String refreshLastUpdate() {

        //save and display last update timestamp
        Calendar calendar = Calendar.getInstance();
        String lastUpdate = MainActivity.standardFormat.format(calendar.getTime());

        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(MainActivity.PREF_LAST_UPDATE, lastUpdate);
        editor.apply();

        return lastUpdate;
    }
}
