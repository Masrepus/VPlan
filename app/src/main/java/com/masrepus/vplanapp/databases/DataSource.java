package com.masrepus.vplanapp.databases;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.timetable.TimetableRow;

/**
 * Used as a user interface for MySQLiteHelper
 */
public class DataSource {

    private SQLiteDatabase database;
    private SQLiteHelper sqLiteHelper;
    private Context context;

    public DataSource(Context context) {
        this.context = context;
        sqLiteHelper = new SQLiteHelper(context, SQLiteHelper.DB_NAME, SQLiteHelper.DB_VERSION);
    }

    public void open() throws SQLException {
        database = sqLiteHelper.getWritableDatabase();
    }

    /**
     * Closes the database
     */
    public void close() {
        sqLiteHelper.close();
    }

    public void createRowTimetable(int day, String lesson, String subject, String room) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_DAY, day);
        values.put(SQLiteHelper.COLUMN_LESSON, lesson);
        values.put(SQLiteHelper.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelper.COLUMN_ROOM, room);

        database.insert(SQLiteHelper.TABLE_TIMETABLE, null, values);
    }

    public void updateRowTimetable(int day, String lesson, String subject, String room, TimetableRow oldData) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_DAY, day);
        values.put(SQLiteHelper.COLUMN_LESSON, lesson);
        values.put(SQLiteHelper.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelper.COLUMN_ROOM, room);

        database.update(SQLiteHelper.TABLE_TIMETABLE, values, SQLiteHelper.COLUMN_LESSON + "='" + oldData.getLesson() + "' AND " + SQLiteHelper.COLUMN_SUBJECT + "='" + oldData.getSubject() + "' AND " + SQLiteHelper.COLUMN_ROOM + "='" + oldData.getRoom() + "'", null);
    }

    public void deleteRowTimetable(int day, String lesson) {
        database.delete(SQLiteHelper.TABLE_TIMETABLE, SQLiteHelper.COLUMN_DAY + "='" + day + "' and " + SQLiteHelper.COLUMN_LESSON + "='" + lesson + "'", null);
    }

    /**
     * Takes care of inserting all columns of a vplan item into its table
     *
     * @param id,classLevel,stunde,klasse,status the columns to insert
     */
    public void createRowVplan(int id, int classLevel, String stunde, String klasse, String status) {

        //create new ContentValues with the column name as key and the cell data as value
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_ID, id);
        values.put(SQLiteHelper.COLUMN_CLASS_LEVEL, classLevel);
        values.put(SQLiteHelper.COLUMN_LESSON, stunde);
        values.put(SQLiteHelper.COLUMN_CLASS, klasse);
        values.put(SQLiteHelper.COLUMN_STATUS, status);

        //find out which db is currently in use
        database.insert(SQLiteHelper.TABLE_VPLAN, null, values);
    }

    /**
     * Inserts the passed values into the available files table
     *
     * @param classLevel,tag,url the columns to insert
     */
    public void createRowLinks(int classLevel, int id, String day, String url) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_CLASS_LEVEL, classLevel);
        values.put(SQLiteHelper.COLUMN_ID, id);
        values.put(SQLiteHelper.COLUMN_DAY, day);
        values.put(SQLiteHelper.COLUMN_URL, url);

        database.insert(SQLiteHelper.TABLE_LINKS, null, values);
    }

    public void createRowAnnouncements(int classLevel, int id, String status) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_CLASS_LEVEL, classLevel);
        values.put(SQLiteHelper.COLUMN_ID, id);
        values.put(SQLiteHelper.COLUMN_STATUS, status);

        database.insert(SQLiteHelper.TABLE_ANNOUNCEMENTS, null, values);
    }

    public void createRowTests(int classLevel, String grade, String day, String subject, String type) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_CLASS_LEVEL, classLevel);
        values.put(SQLiteHelper.COLUMN_DAY, day);
        values.put(SQLiteHelper.COLUMN_SUBJECT, subject);
        values.put(SQLiteHelper.COLUMN_TYPE, type);
        values.put(SQLiteHelper.COLUMN_CLASS, grade);

        database.insert(SQLiteHelper.TABLE_TESTS, null, values);
    }

    public void delete(String tablename, String whereClause) {
        database.delete(tablename, whereClause, null);
    }

    public void delete(String tablename, int vplanMode) {
        database.delete(tablename, SQLiteHelper.COLUMN_CLASS_LEVEL + "=" + vplanMode, null);
    }

    /**
     * Passes queries to the right database by checking which vplan mode is currently active
     *
     *
     * @param distinct
     * @param tableName  the tablename
     * @param projection the columns to query for
     * @return a cursor object containing the queried columns
     */
    public Cursor query(boolean distinct, String tableName, String[] projection) {
        return database.query(
                distinct,
                tableName,
                projection,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor query(boolean distinct, String tableName, String[] projection, String selection) {
        return database.query(
                distinct,
                tableName,
                projection,
                selection,
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor query(boolean distinct, String tableName, String[] projection, int vplanMode) {
        return database.query(
                distinct,
                tableName,
                projection,
                SQLiteHelper.COLUMN_CLASS_LEVEL + "=" + vplanMode,
                null,
                null,
                null,
                null,
                null);
    }

    public void newTable(String tableName) {
        sqLiteHelper.newTable(database, tableName);
    }

    /**
     * Checks whether a specific table contains data
     *
     * @return false if table is empty, otherwise true
     */
    public boolean hasData(String tablename) {
        return DatabaseUtils.queryNumEntries(database, tablename) > 0;
    }
}
