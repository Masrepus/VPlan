package com.masrepus.vplanapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by samuel on 31.01.15.
 */
public class SQLiteHelperTimetable extends SQLiteOpenHelper {

    //Tables for all weekdays
    public static final String TABLE_MONDAY = "monday";
    public static final String TABLE_TUESDAY = "tuesday";
    public static final String TABLE_WEDNESDAY = "wednesday";
    public static final String TABLE_THURSDAY = "thursday";
    public static final String TABLE_FRIDAY = "friday";

    //column names
    public static final String COLUMN_SUBJECT = "subject";
    public static final String COLUMN_LESSON = "lesson";
    public static final String COLUMN_ROOM = "room";

    //Creation statement
    private static final String TABLENAME_WILDCARD = "%TABLE_NAME";
    private static final String CREATE = "create table "
            + TABLENAME_WILDCARD + "(" + COLUMN_LESSON
            + " integer primary key autoincrement, " + COLUMN_SUBJECT
            + " text, " + COLUMN_ROOM + " text);";

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_TIMETABLE = "timetable.db";

    public SQLiteHelperTimetable(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, TABLE_MONDAY));
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, TABLE_TUESDAY));
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, TABLE_WEDNESDAY));
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, TABLE_THURSDAY));
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, TABLE_FRIDAY));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MONDAY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TUESDAY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEDNESDAY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_THURSDAY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIDAY);
        onCreate(db);
    }

    public void newTable(SQLiteDatabase db, String tableName) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, tableName));
    }
}
