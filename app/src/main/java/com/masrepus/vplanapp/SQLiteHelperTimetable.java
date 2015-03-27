package com.masrepus.vplanapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by samuel on 31.01.15.
 */
public class SQLiteHelperTimetable extends SQLiteOpenHelper {

    //Tables for all weekdays
    public static final String[] DAYS = {"monday", "tuesday", "wednesday", "thursday", "friday"};

    //column names
    public static final String COLUMN_SUBJECT = "subject";
    public static final String COLUMN_LESSON = "lesson";
    public static final String COLUMN_ROOM = "room";

    //Creation statement for weekdays
    public static final String TABLENAME_WILDCARD = "%TABLE_NAME";
    public static final String CREATE = "create table "
            + TABLENAME_WILDCARD + "(" + COLUMN_LESSON
            + " integer primary key autoincrement, " + COLUMN_SUBJECT
            + " text, " + COLUMN_ROOM + " text);";

    //subject table for autocomplete
    public static final String TABLE_SUBJECTS_ACTV = "subjectsACTV";
    public static final String COLUMN_ID = "_id";
    public static final String SUBJECT_ACTV_CREATE = "create table "
            + TABLE_SUBJECTS_ACTV + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_SUBJECT
            + " text);";

    //room table for autocomplete
    public static final String TABLE_ROOMS_ACTV = "roomsACTV";
    public static final String ROOMS_ACTV_CREATE = "create table "
            + TABLE_ROOMS_ACTV + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_ROOM
            + " text);";

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_TIMETABLE = "timetable.db";

    public SQLiteHelperTimetable(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (int i = 0; i < 5; i++) {
            db.execSQL(CREATE.replace(TABLENAME_WILDCARD, DAYS[i]));
        }
        db.execSQL(SUBJECT_ACTV_CREATE);
        db.execSQL(ROOMS_ACTV_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = 0; i < 5; i++) {
            db.execSQL("DROP TABLE IF EXISTS " + DAYS[i]);
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS_ACTV);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOMS_ACTV);
        onCreate(db);
    }

    public void newTable(SQLiteDatabase db, String tableName) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        for (String weekday : DAYS) {
            if (weekday.contentEquals(tableName)) db.execSQL(CREATE.replace(TABLENAME_WILDCARD, tableName));
        }

        if (tableName.contentEquals(TABLE_ROOMS_ACTV)) db.execSQL(ROOMS_ACTV_CREATE);
        if (tableName.contentEquals(TABLE_SUBJECTS_ACTV)) db.execSQL(SUBJECT_ACTV_CREATE);
    }
}
