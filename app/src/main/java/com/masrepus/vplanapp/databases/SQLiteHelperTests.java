package com.masrepus.vplanapp.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by samuel on 23.11.14.
 */
public class SQLiteHelperTests extends SQLiteOpenHelper {

    //Tests tables
    public static final String TEST_TABLE_BASIC_NAME = "tests";
    public static final String TABLE_TESTS_UINFO_MINFO = "tests_uinfo_minfo";
    public static final String TABLE_TESTS_OINFO = "tests_oinfo";

    public static final String COLUMN_ID = SQLiteHelperVplan.COLUMN_ID;
    public static final String COLUMN_GRADE = SQLiteHelperVplan.COLUMN_GRADE;
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_SUBJECT = "subject";
    public static final String DATABASE_TESTS = "tests.db";
    private static final String CREATE_UINFO_MINFO = "create table "
            + TABLE_TESTS_UINFO_MINFO + "(" + SQLiteHelperVplan.COLUMN_ID
            + " integer primary key autoincrement, " + SQLiteHelperVplan.COLUMN_GRADE
            + " text, " + COLUMN_TYPE + " text, " + COLUMN_SUBJECT + " text, " + COLUMN_DATE + " text);";
    private static final String CREATE_OINFO = "create table "
            + TABLE_TESTS_OINFO + "(" + SQLiteHelperVplan.COLUMN_ID
            + " integer primary key autoincrement, " + SQLiteHelperVplan.COLUMN_GRADE
            + " text, " + COLUMN_TYPE + " text, " + COLUMN_SUBJECT + " text, " + COLUMN_DATE + " text);";
    private static final int DATABASE_VERSION = 1;


    public SQLiteHelperTests(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_UINFO_MINFO);
        database.execSQL(CREATE_OINFO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TESTS_UINFO_MINFO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TESTS_OINFO);
        onCreate(db);
    }

    public void newTable(SQLiteDatabase db, String tableName) {
        if (tableName.contentEquals(TABLE_TESTS_UINFO_MINFO)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TESTS_UINFO_MINFO);
            db.execSQL(CREATE_UINFO_MINFO);
        }
        if (tableName.contentEquals(TABLE_TESTS_OINFO)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TESTS_OINFO);
            db.execSQL(CREATE_OINFO);
        }
    }
}
