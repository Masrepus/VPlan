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

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * The async task used for background-parsing of online data
 */
public class AsyncDownloader extends AsyncTask<Context, Enum, Boolean> {

    private static final int BASIC = MainActivity.BASIC;
    private static final int UINFO = MainActivity.UINFO;
    private static final int MINFO = MainActivity.MINFO;
    private static final int OINFO = MainActivity.OINFO;
    ProgressCode progress;
    int downloaded;
    int total_downloads;
    Context context;
    ProgressBar progressBar;
    VPlanDataSource datasource;
    private int requestedVplanId;
    private String currentVPlanLink;
    private String timePublished;
    private int requestedVplanMode;
    private int appMode;
    private String grade;
    private ArrayList<ArrayList<String>> filters;
    private boolean downloaded11 = false;
    private boolean downloaded12 = false;
    private boolean downloadedUinfoMinfo = false;

    protected int getRequestedVplanMode() {
        //externalised so that the service can override this
        SharedPreferences pref = this.context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return pref.getInt(MainActivity.PREF_VPLAN_MODE, UINFO);
    }

    protected int getAppMode() {
        //possibility of overriding
        /*SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return pref.getInt(MainActivity.PREF_APPMODE, MainActivity.VPLAN);*/
        return MainActivity.VPLAN;
    }

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

        //get the requested vplan mode
        requestedVplanMode = getRequestedVplanMode();

        //get the appmode
        appMode = getAppMode();

        switch (appMode) {

            case MainActivity.VPLAN:
                return downloadVplan(editor);
            case MainActivity.TESTS:
                return downloadTests();
            default:
                return downloadVplan(editor);
        }

    }

    private boolean downloadTests() {

        publishProgress(ProgressCode.STARTED);
        downloaded = 0;
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int vplanModeBefore = pref.getInt(MainActivity.PREF_VPLAN_MODE, MainActivity.UINFO);
        SharedPreferences.Editor editor = pref.edit();

        //get the filter sets
        filters = new ArrayList<ArrayList<String>>();
        filters.add(new ArrayList<String>(pref.getStringSet(context.getString(R.string.pref_key_filter_uinfo), null)));

        filters.add(new ArrayList<String>(pref.getStringSet(context.getString(R.string.pref_key_filter_minfo), null)));

        filters.add(new ArrayList<String>(pref.getStringSet(context.getString(R.string.pref_key_filter_oinfo), null)));

        //get total downloads to do
        for (int i=0; i<2; i++) {
            total_downloads += filters.get(i).size();
        }

        if (total_downloads == 0 && !(filters.get(2).size() > 0)) {
            //there are no filtered classes so no need to download anything; notify user!
            publishProgress(ProgressCode.NOTHING_TO_DOWNLOAD);
        }

        //clear all existing data
        datasource.open();
        datasource.newTable(SQLiteHelperTests.TABLE_TESTS_OINFO);
        datasource.newTable(SQLiteHelperTests.TABLE_TESTS_UINFO_MINFO);
        datasource.close();

        //call parseTestsToSql for each filtered grade/course
        for (ArrayList<String> currFilter : filters) {

            for (String currGrade : currFilter) {
                grade = currGrade;
                try {
                    if (currFilter == filters.get(2)) {
                        //oinfo

                        requestedVplanMode = OINFO;
                        if (grade.charAt(0) == '1') {
                            grade = "11"; //1XY means grade 11, 2XY grade 12
                            //check whether this grade has been downloaded already
                            if (downloaded11) continue;
                            else total_downloads++;
                            parseOinfoTests();
                            downloaded11 = true;
                        }
                        else {
                            grade = "12";
                            if (downloaded12) continue;
                            else total_downloads++;
                            parseOinfoTests();
                            downloaded12 = true;
                        }
                    }
                    else if (currFilter == filters.get(0)){
                        //uinfo or minfo
                        requestedVplanMode = UINFO; //this is important for findRequestedTestsPage
                        parseUinfoMinfoTests(currGrade);
                    } else {
                        requestedVplanMode = MINFO;
                        parseUinfoMinfoTests(currGrade);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e.getMessage() != null) {
                        if (e.getMessage().contentEquals("failed to connect oinfo")) {
                            publishProgress(ProgressCode.ERR_NO_INTERNET);
                        }
                    }
                    return false;
                }
                downloaded++;
                publishProgress(ProgressCode.PARSING_FINISHED);
            }
        }

        //reset shared prefs
        requestedVplanMode = vplanModeBefore;

        publishProgress(ProgressCode.FINISHED_ALL);

        return true;
    }

    private boolean downloadVplan(SharedPreferences.Editor editor) {

        //download new data and then refresh pager adapter

        try {
            publishProgress(ProgressCode.STARTED);
            updateAvailableFilesList();
        } catch (Exception e) {
            //check whether this is because of missing creds
            if (e.getMessage().contentEquals("failed to connect without creds")) {
                publishProgress(ProgressCode.ERR_NO_CREDS);
                return false;
            } else if (e.getMessage().contentEquals("failed to connect")) {
                publishProgress(ProgressCode.ERR_NO_INTERNET_OR_NO_CREDS);
                return false;
            } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                publishProgress(ProgressCode.ERR_NO_INTERNET);
                return false;
            }
        }

        if (isCancelled()) return false;

        datasource.open();

        Cursor c = datasource.query(MySQLiteHelper.TABLE_LINKS, new String[]{MySQLiteHelper.COLUMN_URL});

        try {
            total_downloads = c.getCount();
            downloaded = 0;

            while (c.moveToNext()) {

                if (isCancelled()) return false;

                //load every available vplan into the db
                requestedVplanId = c.getPosition();
                currentVPlanLink = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_URL));

                editor.putInt(MainActivity.PREF_REQUESTED_VPLAN_ID, requestedVplanId)
                        .putString(MainActivity.PREF_CURR_VPLAN_LINK, currentVPlanLink);
                editor.apply();

                parseDataToSql();

                downloaded = c.getPosition() + 1;
                publishProgress(ProgressCode.PARSING_FINISHED);
            }

            publishProgress(ProgressCode.FINISHED_ALL);

            datasource.close();

            return true;
        } catch (Exception e) {

            //check whether this is because of missing creds
            if (e.getMessage().contentEquals("failed to connect without creds")) {
                publishProgress(ProgressCode.ERR_NO_CREDS);
            } else if (e.getMessage().contentEquals("failed to connect")) {
                publishProgress(ProgressCode.ERR_NO_INTERNET_OR_NO_CREDS);
            } else if (e.getMessage().contentEquals("failed to connect oinfo")) {
                publishProgress(ProgressCode.ERR_NO_INTERNET);
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

    public void parseUinfoMinfoTests(String grade) throws Exception {

        Document doc;

        try {
            doc = Jsoup.connect(findRequestedTestsPage()).get();
        } catch (HttpStatusException e) {
            throw new Exception("failed to connect oinfo");
        }

        Element list = doc.select("li").first();

        if (list != null) {

            //get the nodes containing the relevant test information
            List<TextNode> testNodes = list.textNodes();

            datasource.open();

            //check whether existing test data must be wiped because this is the first round
            if (!downloadedUinfoMinfo) {
                datasource.newTable(SQLiteHelperTests.TABLE_TESTS_UINFO_MINFO);
            }

            for (TextNode node : testNodes) {

                String completeText = node.text();

                if (!(completeText.toCharArray().length == 1)) {

                    //separate date and subject which are connected by ':'
                    String[] split = completeText.split(":");
                    String date = split[0];

                    String[] split2 = split[1].trim().split(" ");
                    String type = split2[0].trim();

                    //find position of this test's type in types-array and give it the correct abbreviation
                    ArrayList<String> types = new ArrayList<String>(Arrays.asList(context.getResources().getStringArray(R.array.test_types)));
                    ArrayList<String> types_short = new ArrayList<String>(Arrays.asList(context.getResources().getStringArray(R.array.test_types_short)));
                    if (types.contains(type)) {
                        type = types_short.get(types.indexOf(type));
                    }

                    String subject = split2[2].trim();

                    //rest goes into subject
                    for (int i = 3; i < split2.length; i++) {
                        subject += " " + split2[i];
                    }

                    subject = subject.replace('\uFFFD', 'ö');

                    datasource.createRowTests(SQLiteHelperTests.TABLE_TESTS_UINFO_MINFO, grade, date, subject, type);
                }
            }

            datasource.close();
        }

        downloadedUinfoMinfo = true;
        publishProgress(ProgressCode.PARSING_FINISHED);
    }

    public void parseOinfoTests() throws Exception {

        Document doc;

        try {
            doc = Jsoup.connect(findRequestedTestsPage()).get();
        } catch (IOException e) {
            throw new Exception("failed to connect oinfo");
        }

        //get the table element
        Element list = doc.select("li").first();
        Element table = list.child(0);

        //get the last updated timestamp
        String lastUpdate = list.textNodes().get(2).text();

        Elements tableRows = table.child(0).children();
        parseOinfoTestData(tableRows);

        publishProgress(ProgressCode.PARSING_FINISHED);
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

        //sql insert of all three columns, but only if they aren't all empty or header items

        if (stunde != null && !klasse.contentEquals("Klasse")) {

            if (requestedVplanId <= 4) {
                datasource.createRowVplan(MySQLiteHelper.tablesVplan[requestedVplanId], position, stunde, klasse, status);
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

        //now save the current loaded vplan's date and its last-changed timestamp, each including vplanmode, for later usage
        SharedPreferences pref = this.context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(MainActivity.PREF_PREFIX_VPLAN_CURR_DATE + String.valueOf(requestedVplanMode) + String.valueOf(requestedVplanId), currentDate);
        editor.putString(MainActivity.PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanMode) + String.valueOf(requestedVplanId), timePublished);
        editor.apply();
    }

    public void parseOinfoTestData(Elements tableRows) {

        if (tableRows != null) {

            String currDate = "";

            datasource.open();

            //check whether existing test data must be wiped because this is the first round
            if (!downloaded11 && !downloaded12) {
                datasource.newTable(SQLiteHelperTests.TABLE_TESTS_OINFO);
            }

            //iterate through the rows
            for (Element row : tableRows) {

                String[] columns = new String[row.children().size()];
                String date;
                String course;

                //distribute the row's content into an array in order to get the columns
                for (int i = 0; i < row.children().size(); i++) {
                    columns[i] = row.child(i).text();
                }

                //put the data into the right strings
                if (columns.length == 2) { //this makes it skip the title row
                    date = columns[0];
                    course = columns[1];

                    if (date.contentEquals("\u00a0")) {

                        //we have to insert the current date manually as the same date is never repeated in the table
                        date = currDate;
                    } else currDate = date;

                    String grade;
                    //set the grade according to the first char in subject
                    if ("1".contentEquals(String.valueOf(course.charAt(0)))) grade = "Q11";
                    else if (String.valueOf(course.charAt(0)).contentEquals("2")) grade = "Q12";
else grade ="Q11/12"; //those other courses belong to both 11 and 12

                    //perform filtering
                    boolean isNeeded = false;
                    for (String currGrade : filters.get(2)) {
                        if (currGrade.toLowerCase().contentEquals(course)) {
                            isNeeded = true;
                            break;
                        }
                    }

                    //now check whether we got this in the db already (5xy courses are treated as grade 12)
                    Cursor c = datasource.query(SQLiteHelperTests.TABLE_TESTS_OINFO, new String[]{SQLiteHelperTests.COLUMN_SUBJECT}, SQLiteHelperTests.COLUMN_SUBJECT + " = " + "'" + course + "'");
                    if (c.getCount() > 0) isNeeded = false;

                    //sql insert, but skip this if the course column is empty
                    if (!course.contentEquals("\u00a0") && isNeeded) {
                        datasource.createRowTests(SQLiteHelperTests.TABLE_TESTS_OINFO, grade, date, course, context.getString(R.string.standard_test_abbrev)); //in oinfo, all tests are of the same type and grade is not relevant
                    }
                }
            }
            datasource.close();
        }
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

        if (!url.contentEquals("")) return url;
        else return getVPlanUrl(MainActivity.UINFO, false);
    }

    private String findRequestedTestsPage() {

        //return the right url for the requested mode and grade: u/minfo have the same, only oinfo has a different one
        String url = "";

        switch (requestedVplanMode) {

            case UINFO:
            case MINFO:
                url = context.getString(R.string.tests_base_url) + grade;
                break;
            case OINFO:
                url = context.getString(R.string.tests_base_oinfo) + grade + ".html";
                break;
        }

        if (!url.contentEquals("")) return url;
        else return context.getString(R.string.tests_base_url) + grade;
    }

    public static String findRequestedTestsPage(Context context, int mode) {

        //return the right url for the requested mode and grade: u/minfo have the same, only oinfo has a different one
        String url = "";

        switch (mode) {

            case UINFO:
            case MINFO:
                url = context.getString(R.string.tests_base_url);
                break;
            case OINFO:
                url = context.getString(R.string.vplan_base_url) + "oinfo/" + "srekursiv.php";
                break;
        }

        if (!url.contentEquals("")) return url;
        else return context.getString(R.string.tests_base_url);
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
