package com.masrepus.vplanapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;

/**
 * Used as a user interface for MySQLiteHelper
 */
public class VPlanDataSource {

    //Database fields
    private SQLiteDatabase databaseUinfo;
    private SQLiteDatabase databaseMinfo;
    private SQLiteDatabase databaseOinfo;
    private SQLiteDatabase databaseTests;

    private MySQLiteHelper dbHelperUinfo;
    private MySQLiteHelper dbHelperMinfo;
    private MySQLiteHelper dbHelperOinfo;
    private SQLiteHelperTests dbHelperTests;

    private Context context;

    /**
     * Creates instances of MySQLiteHelper for U/M/Oinfo
     *
     * @param context passed to MySQLiteHelper constructor
     */
    public VPlanDataSource(Context context) {
        this.context = context;
        dbHelperUinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_UINFO);
        dbHelperMinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_MINFO);
        dbHelperOinfo = new MySQLiteHelper(context, MySQLiteHelper.DATABASE_OINFO);
        dbHelperTests = new SQLiteHelperTests(context, SQLiteHelperTests.DATABASE_TESTS);
    }

    /**
     * Creates writable instances of all three dbs
     */
    public void open() throws SQLException {
        databaseUinfo = dbHelperUinfo.getWritableDatabase();
        databaseMinfo = dbHelperMinfo.getWritableDatabase();
        databaseOinfo = dbHelperOinfo.getWritableDatabase();
        databaseTests = dbHelperTests.getWritableDatabase();
    }

    /**
     * Closes all dbs
     */
    public void close() {
        dbHelperUinfo.close();
        dbHelperMinfo.close();
        dbHelperOinfo.close();
        dbHelperTests.close();
    }

    /**
     * Takes care of inserting all columns of a vplan item into its table
     *
     * @param tableName the table where the columns will be inserted
     * @param stunde,klasse,status the columns to insert
     */
    public void createRowVplan(String tableName, String stunde, String klasse, String status) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_STUNDE, stunde);
        values.put(MySQLiteHelper.COLUMN_GRADE, klasse);
        values.put(MySQLiteHelper.COLUMN_STATUS, status);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
            case VplanModes.UINFO:
                databaseUinfo.insert(tableName, null, values);
                break;
            case VplanModes.MINFO:
                databaseMinfo.insert(tableName, null, values);
                break;
            case VplanModes.OINFO:
                databaseOinfo.insert(tableName, null, values);
                break;
        }
    }

    /**
     * Inserts the passed values into the available files table
     *
     * @param id,tag,url the columns to insert
     */
    public void createRowLinks(Integer id, String tag, String url) {

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_ID, id);
        values.put(MySQLiteHelper.COLUMN_TAG, tag);
        values.put(MySQLiteHelper.COLUMN_URL, url);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
            case VplanModes.UINFO:
                databaseUinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
            case VplanModes.MINFO:
                databaseMinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
            case VplanModes.OINFO:
                databaseOinfo.insert(MySQLiteHelper.TABLE_LINKS, null, values);
                break;
        }
    }

    public void createRowTests(String tablename, String grade, String date, String subject, String type) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelperTests.COLUMN_DATE, date);
        values.put(SQLiteHelperTests.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelperTests.COLUMN_TYPE, type);
        values.put(SQLiteHelperTests.COLUMN_GRADE, grade);

        databaseTests.insert(tablename, null, values);
    }

    /**
     * Passes queries to the right database by checking which vplan mode is currently active
     *
     * @param tableName       the tablename
     * @param projection the columns to query for
     * @return a cursor object containing the queried columns
     */
    public Cursor query(String tableName, String[] projection) {

        //check whether we have to query the tests db
        if (tableName.contains(SQLiteHelperTests.TEST_TABLE_BASIC_NAME)) {
            return databaseTests.query(
                    tableName,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null);
        } else {

            //find out which db is currently in use
            SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
            switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
                case VplanModes.UINFO:
                    return databaseUinfo.query(
                            tableName,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null);
                case VplanModes.MINFO:
                    return databaseMinfo.query(
                            tableName,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null);
                case VplanModes.OINFO:
                    return databaseOinfo.query(
                            tableName,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null);
                default:
                    return databaseUinfo.query(
                            tableName,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null);
            }
        }
    }

    public Cursor query(String tableName, String[] projection, String selection) {

        //check whether we have to query the tests db
        if (tableName.contains(SQLiteHelperTests.TEST_TABLE_BASIC_NAME)) {
            return databaseTests.query(
                    tableName,
                    projection,
                    selection,
                    null,
                    null,
                    null,
                    null);
        } else {

            //find out which db is currently in use
            SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
            switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
                case VplanModes.UINFO:
                    return databaseUinfo.query(
                            tableName,
                            projection,
                            selection,
                            null,
                            null,
                            null,
                            null);
                case VplanModes.MINFO:
                    return databaseMinfo.query(
                            tableName,
                            projection,
                            selection,
                            null,
                            null,
                            null,
                            null);
                case VplanModes.OINFO:
                    return databaseOinfo.query(
                            tableName,
                            projection,
                            selection,
                            null,
                            null,
                            null,
                            null);
                default:
                    return databaseUinfo.query(
                            tableName,
                            projection,
                            selection,
                            null,
                            null,
                            null,
                            null);
            }
        }
    }

    /**
     * Passes requests to the right MySQLiteHelper object to re-create a specific table
     */
    public void newTable(String tableName) {

        //check whether this is a tests table
        if (tableName.contains(SQLiteHelperTests.TEST_TABLE_BASIC_NAME)) {
            dbHelperTests.newTable(databaseTests, tableName);
        } else {
            //find out which db is currently in use
            SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
            switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
                case VplanModes.UINFO:
                    dbHelperUinfo.newTable(databaseUinfo, tableName);
                    break;
                case VplanModes.MINFO:
                    dbHelperMinfo.newTable(databaseMinfo, tableName);
                    break;
                case VplanModes.OINFO:
                    dbHelperOinfo.newTable(databaseOinfo, tableName);
                    break;
            }
        }
    }

    /**
     * Checks whether a specific table contains data by querying for _id columns. If that cursor is empty, the table must be empty, too.
     *
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
