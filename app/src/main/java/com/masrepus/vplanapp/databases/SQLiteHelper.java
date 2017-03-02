package com.masrepus.vplanapp.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Samuel on 22.02.2017.
 */

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "vplan.db";

    public static final String TABLE_VPLAN = "vplantable";
    public static final String TABLE_ANNOUNCEMENTS = "announcements";
    public static final String TABLE_TESTS = "tests";
    public static final String TABLE_TIMETABLE = "timetable";
    public static final String TABLE_LINKS = "links";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CLASS = "grade";
    public static final String COLUMN_LESSON = "lesson";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_CLASS_LEVEL = "classLevel";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_SUBJECT = "subject";
    public static final String COLUMN_ROOM = "room";
    public static final String COLUMN_URL = "url";

    //table creation statements

    //vplan
    private static final String VPLAN_CREATE = "create table " + TABLE_VPLAN + "("
            + COLUMN_ID + " integer, "
            + COLUMN_CLASS_LEVEL + " integer, "
            + COLUMN_CLASS + " text, "
            + COLUMN_LESSON + " text, "
            + COLUMN_STATUS + " text);";

    //announcements
    private static final String ANNOUNCEMENTS_CREATE = "create table " + TABLE_ANNOUNCEMENTS + "("
            + COLUMN_CLASS_LEVEL + " integer, "
            + COLUMN_ID + " integer, "
            + COLUMN_STATUS + " text);";

    //tests
    private static final String TESTS_CREATE = "create table " + TABLE_TESTS + "("
            + COLUMN_CLASS_LEVEL + " integer, "
            + COLUMN_DAY + " text, "
            + COLUMN_CLASS + " text, "
            + COLUMN_SUBJECT + " text, "
            + COLUMN_TYPE + " type);";

    //timetable
    private static final String TIMETABLE_CREATE = "create table " + TABLE_TIMETABLE + "("
            + COLUMN_DAY + " integer, "
            + COLUMN_LESSON + " text, "
            + COLUMN_SUBJECT + " text, "
            + COLUMN_ROOM + " text);";

    //links
    private static final String LINKS_CREATE = "create table " + TABLE_LINKS + "("
            + COLUMN_CLASS_LEVEL + " integer, "
            + COLUMN_ID + " integer, "
            + COLUMN_DAY + " text, "
            + COLUMN_URL + " text);";

    private Context context;

    public SQLiteHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(VPLAN_CREATE);
        sqLiteDatabase.execSQL(ANNOUNCEMENTS_CREATE);
        sqLiteDatabase.execSQL(TESTS_CREATE);
        sqLiteDatabase.execSQL(TIMETABLE_CREATE);
        sqLiteDatabase.execSQL(LINKS_CREATE);

        //migrate old data to the new db, if it existed before
        migrateData(sqLiteDatabase);
    }

    private void migrateData(SQLiteDatabase newDatabase) {
        //only timetable data matters, everything else can be downloaded
        //look for a legacy timetable db
        try {
            SQLiteDatabase legacyTimetable = SQLiteDatabase.openDatabase(context.getDatabasePath("timetable.db").getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            Log.d(DB_NAME, "started migrating timetable");

            String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday"};

            for (int i = 0; i < 5; i++) {
                //query each weekday table and insert into the new timetable table
                Cursor c = legacyTimetable.query(days[i], null, null, null, null, null, null);

                while (c.moveToNext()) {
                    //insert each row into the new timetable
                    ContentValues row = new ContentValues();
                    row.put(COLUMN_DAY, i);
                    row.put(COLUMN_LESSON, c.getString(c.getColumnIndex(COLUMN_LESSON)));
                    row.put(COLUMN_SUBJECT, c.getString(c.getColumnIndex(COLUMN_SUBJECT)));
                    row.put(COLUMN_ROOM, c.getString(c.getColumnIndex(COLUMN_ROOM)));

                    newDatabase.insert(TABLE_TIMETABLE, null, row);
                    Log.d(DB_NAME, "inserted timetable row " + row.toString());
                }

                c.close();
            }

            legacyTimetable.close();

            //now delete all old db's
            context.deleteDatabase("timetable.db");
            Log.d(DB_NAME, "deleted timetable.db");
            context.deleteDatabase("uinfo.db");
            Log.d(DB_NAME, "deleted uinfo.db");
            context.deleteDatabase("minfo.db");
            Log.d(DB_NAME, "deleted minfo.db");
            context.deleteDatabase("oinfo.db");
            Log.d(DB_NAME, "deleted oinfo.db");
            context.deleteDatabase("tests.db");
            Log.d(DB_NAME, "deleted tests.db");
        } catch (SQLException e) {
            //no legacy db found, great!
            Log.d(DB_NAME, "no legacy db found");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void newTable(SQLiteDatabase db, String tablename) {

        db.execSQL("drop table if exists " + tablename);

        //recreate the deleted table
        switch (tablename) {
            case TABLE_ANNOUNCEMENTS:
                db.execSQL(ANNOUNCEMENTS_CREATE);
                break;
            case TABLE_TESTS:
                db.execSQL(TESTS_CREATE);
                break;
            case TABLE_TIMETABLE:
                db.execSQL(TIMETABLE_CREATE);
                break;
            case TABLE_VPLAN:
                db.execSQL(VPLAN_CREATE);
                break;
            case TABLE_LINKS:
                db.execSQL(LINKS_CREATE);
                break;
        }
    }
}