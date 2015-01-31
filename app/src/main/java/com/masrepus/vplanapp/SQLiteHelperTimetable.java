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
        for (int i = 0; i < 5; i++) {
            db.execSQL(CREATE.replace(TABLENAME_WILDCARD, DAYS[i]));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = 0; i < 5; i++) {
            db.execSQL("DROP TABLE IF EXISTS " + DAYS[i]);
        }
        onCreate(db);
    }

    public void newTable(SQLiteDatabase db, String tableName) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.execSQL(CREATE.replace(TABLENAME_WILDCARD, tableName));
    }
}
