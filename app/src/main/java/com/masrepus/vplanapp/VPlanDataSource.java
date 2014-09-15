package com.masrepus.vplanapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Used as a user interface for MySQLiteHelper
 */
public class VPlanDataSource {

    //Database fields
    private SQLiteDatabase databaseUinfo;
    private SQLiteDatabase databaseMinfo;
    private SQLiteDatabase databaseOinfo;

    private MySQLiteHelper dbHelperUinfo;
    private MySQLiteHelper dbHelperMinfo;
    private MySQLiteHelper dbHelperOinfo;

    private Context context;

    /**
     * Creates instances of MySQLiteHelper for U/M/Oinfo
     * @param context passed to MySQLiteHelper constructor
     */
    public VPlanDataSource(Context context) {
        this.context = context;
        dbHelperUinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_UINFO);
        dbHelperMinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_MINFO);
        dbHelperOinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_OINFO);
    }

    /**
     * Creates writable instances of all three dbs
     */
    public void open() throws SQLException {
        databaseUinfo = dbHelperUinfo.getWritableDatabase();
        databaseMinfo = dbHelperMinfo.getWritableDatabase();
        databaseOinfo = dbHelperOinfo.getWritableDatabase();
    }

    /**
     * Closes all dbs
     */
    public void close() {
        dbHelperUinfo.close();
        dbHelperMinfo.close();
        dbHelperOinfo.close();
    }

    /**
     * Takes care of inserting all columns of a vplan item into its table
     * @param tableName the table where the columns will be inserted
     * @param id,stunde,klasse,status the columns to insert
     */
    public void createRowVplan(String tableName, Integer id, String stunde, String klasse, String status) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_STUNDE, stunde);
        values.put(MySQLiteHelper.COLUMN_KLASSE, klasse);
        values.put(MySQLiteHelper.COLUMN_ID, id);
        values.put(MySQLiteHelper.COLUMN_STATUS, status);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        switch (pref.getInt(MainActivity.PREF_VPLAN_MODE, 0)) {
            case MainActivity.UINFO:
                databaseUinfo.insert(tableName, null, values);
                break;
            case MainActivity.MINFO:
                databaseMinfo.insert(tableName, null, values);
                break;
            case MainActivity.OINFO:
                databaseOinfo.insert(tableName, null, values);
                break;
        }
    }

    /**
     * Inserts the passed values into the available files table
     * @param id,tag,url the columns to insert
     */
    public void createRowLinks(Integer id, String tag, String url) {

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_ID, id);
        values.put(MySQLiteHelper.COLUMN_TAG, tag);
        values.put(MySQLiteHelper.COLUMN_URL, url);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        switch (pref.getInt(MainActivity.PREF_VPLAN_MODE, 0)) {
            case MainActivity.UINFO:
                databaseUinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
            case MainActivity.MINFO:
                databaseMinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
            case MainActivity.OINFO:
                databaseOinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
        }
    }

    public void createRowTests(String date, String klasse) {

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_DATE, date);
        values.put(MySQLiteHelper.COLUMN_KLASSE, klasse);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        switch (pref.getInt(MainActivity.PREF_VPLAN_MODE, 0)) {
            case MainActivity.UINFO:
                databaseUinfo.insert(MySQLiteHelper.TABLE_TESTS, null, values);
                break;
            case MainActivity.MINFO:
                databaseMinfo.insert(MySQLiteHelper.TABLE_TESTS, null, values);
                break;
            case MainActivity.OINFO:
                databaseOinfo.insert(MySQLiteHelper.TABLE_TESTS, null, values);
                break;
        }
    }

    /**
     * Passes queries to the right database by checking which vplan mode is currently active
     * @param name the table name
     * @param projection the columns to query for
     * @return a cursor object containing the queried columns
     */
    public Cursor query(String name, String[] projection) {

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        switch (pref.getInt(MainActivity.PREF_VPLAN_MODE, 0)) {
            case MainActivity.UINFO:
                return databaseUinfo.query(
                        name,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null);
            case MainActivity.MINFO:
                return databaseMinfo.query(
                        name,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null);
            case MainActivity.OINFO:
                return databaseOinfo.query(
                        name,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null);
            default:
                return databaseUinfo.query(
                        name,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null);
        }
    }

    /**
     * Passes requests to the right MySQLiteHelper object to re-create a specific table
     */
    public void newTable(String tableName) {
        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        switch (pref.getInt(MainActivity.PREF_VPLAN_MODE, 0)) {
            case MainActivity.UINFO:
                dbHelperUinfo.newTable(databaseUinfo, tableName);
                break;
            case MainActivity.MINFO:
                dbHelperMinfo.newTable(databaseMinfo, tableName);
                break;
            case MainActivity.OINFO:
                dbHelperOinfo.newTable(databaseOinfo, tableName);
                break;
        }
    }

    /**
     * Checks whether a specific table contains data by querying for _id columns. If that cursor is empty, the table must be empty, too.
     * @return false if table is empty, otherwise true
     */
    public boolean hasData(String tablename) {
        //return true if table VPlan is filled, false if it is empty/doesn't exist
        //query for available _id columns; if that cursor is empty, then there is no data in the table

        String[] test = new String[1];
        test[0] = MySQLiteHelper.COLUMN_ID;
        Cursor c = query(tablename, test);
        if (c.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
