package com.masrepus.vplanapp.databases;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.timetable.TimetableRow;

/**
 * Used as a user interface for MySQLiteHelper
 */
public class DataSource {

    //Database fields
    private SQLiteDatabase databaseUinfo;
    private SQLiteDatabase databaseMinfo;
    private SQLiteDatabase databaseOinfo;
    private SQLiteDatabase databaseTests;
    private SQLiteDatabase databaseTimetable;

    private SQLiteHelperVplan dbHelperUinfo;
    private SQLiteHelperVplan dbHelperMinfo;
    private SQLiteHelperVplan dbHelperOinfo;
    private SQLiteHelperTests dbHelperTests;
    private SQLiteHelperTimetable dbHelperTimetable;

    private Context context;

    public DataSource(Context context) {
        this.context = context;
        dbHelperUinfo = new SQLiteHelperVplan(context, SQLiteHelperVplan.DATABASE_UINFO);
        dbHelperMinfo = new SQLiteHelperVplan(context, SQLiteHelperVplan.DATABASE_MINFO);
        dbHelperOinfo = new SQLiteHelperVplan(context, SQLiteHelperVplan.DATABASE_OINFO);
        dbHelperTests = new SQLiteHelperTests(context, SQLiteHelperTests.DATABASE_TESTS);
        dbHelperTimetable = new SQLiteHelperTimetable(context, SQLiteHelperTimetable.DATABASE_TIMETABLE);
    }

    public void open() throws SQLException {
        databaseUinfo = dbHelperUinfo.getWritableDatabase();
        databaseMinfo = dbHelperMinfo.getWritableDatabase();
        databaseOinfo = dbHelperOinfo.getWritableDatabase();
        databaseTests = dbHelperTests.getWritableDatabase();
        databaseTimetable = dbHelperTimetable.getWritableDatabase();
    }

    /**
     * Closes all dbs
     */
    public void close() {
        dbHelperUinfo.close();
        dbHelperMinfo.close();
        dbHelperOinfo.close();
        dbHelperTests.close();
        dbHelperTimetable.close();
    }

    public void createRowTimetable(String tableName, String lesson, String subject, String room) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelperTimetable.COLUMN_LESSON, lesson);
        values.put(SQLiteHelperTimetable.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelperTimetable.COLUMN_ROOM, room);

        databaseTimetable.insert(tableName, null, values);
    }

    public void updateRowTimetable(String tableName, String lesson, String subject, String room, TimetableRow oldData) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelperTimetable.COLUMN_LESSON, lesson);
        values.put(SQLiteHelperTimetable.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelperTimetable.COLUMN_ROOM, room);

        databaseTimetable.update(tableName, values, SQLiteHelperTimetable.COLUMN_LESSON + "='" + oldData.getLesson() + "' AND " + SQLiteHelperTimetable.COLUMN_SUBJECT + "='" + oldData.getSubject() + "' AND " + SQLiteHelperTimetable.COLUMN_ROOM + "='" + oldData.getRoom() + "'", null);
    }

    public void deleteRowTimetable(String tableName, String lesson) {

        databaseTimetable.delete(tableName, SQLiteHelperTimetable.COLUMN_LESSON + "='" + lesson + "'", null);
    }

    public void addSubject(String subject) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelperTimetable.COLUMN_SUBJECT, subject);

        databaseTimetable.insert(SQLiteHelperTimetable.TABLE_SUBJECTS_ACTV, null, values);
    }

    public void addRoom(String room) {
        ContentValues values = new ContentValues();
        values.put(SQLiteHelperTimetable.COLUMN_ROOM, room);

        databaseTimetable.insert(SQLiteHelperTimetable.TABLE_ROOMS_ACTV, null, values);
    }

    /**
     * Takes care of inserting all columns of a vplan item into its table
     *
     * @param tableName            the table where the columns will be inserted
     * @param stunde,klasse,status the columns to insert
     */
    public void createRowVplan(String tableName, String stunde, String klasse, String status) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelperVplan.COLUMN_STUNDE, stunde);
        values.put(SQLiteHelperVplan.COLUMN_GRADE, klasse);
        values.put(SQLiteHelperVplan.COLUMN_STATUS, status);

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
        values.put(SQLiteHelperVplan.COLUMN_ID, id);
        values.put(SQLiteHelperVplan.COLUMN_TAG, tag);
        values.put(SQLiteHelperVplan.COLUMN_URL, url);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
            case VplanModes.UINFO:
                databaseUinfo.insert(SQLiteHelperVplan.TABLE_LINKS, null, values);
                break;
            case VplanModes.MINFO:
                databaseMinfo.insert(SQLiteHelperVplan.TABLE_LINKS, null, values);
                break;
            case VplanModes.OINFO:
                databaseOinfo.insert(SQLiteHelperVplan.TABLE_LINKS, null, values);
                break;
        }
    }

    public void createRowAnnouncements(Integer id, String announcement) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelperVplan.COLUMN_ID, id);
        values.put(SQLiteHelperVplan.COLUMN_ANNOUNCEMENT, announcement);

        //find out which db is currently in use
        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        switch (pref.getInt(SharedPrefs.VPLAN_MODE, 0)) {
            case VplanModes.UINFO:
                databaseUinfo.insert(SQLiteHelperVplan.TABLE_ANNOUNCEMENTS, null, values);
                break;
            case VplanModes.MINFO:
                databaseMinfo.insert(SQLiteHelperVplan.TABLE_ANNOUNCEMENTS, null, values);
                break;
            case VplanModes.OINFO:
                databaseOinfo.insert(SQLiteHelperVplan.TABLE_ANNOUNCEMENTS, null, values);
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
     * @param tableName  the tablename
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

    public Cursor queryTimetable(String tableName, String[] projection) {

        return databaseTimetable.query(
                tableName,
                projection,
                null,
                null,
                null,
                null,
                null
        );
    }

    public Cursor queryTimetable(String tableName, String[] projection, String selection) {


        return databaseTimetable.query(
                tableName,
                projection,
                selection,
                null,
                null,
                null,
                null
        );
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

    public void newTimetable(String tableName) {
        dbHelperTimetable.newTable(databaseTimetable, tableName);
    }

    public boolean hasTimetableData(String tableName) {

        //return true if this timetable has data
        String[] test = new String[1];
        test[0] = SQLiteHelperTimetable.COLUMN_LESSON;
        Cursor c = query(tableName, test);
        return c.getCount() > 0;
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
        test[0] = SQLiteHelperVplan.COLUMN_ID;
        Cursor c = query(tablename, test);
        return c.getCount() > 0;
    }
}
