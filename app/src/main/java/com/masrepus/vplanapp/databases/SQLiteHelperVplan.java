package com.masrepus.vplanapp.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelperVplan extends SQLiteOpenHelper {

    public static final String TABLE_VPLAN_0 = "vplantable0";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_GRADE = "klasse";
    public static final String COLUMN_STUNDE = "stunde";
    public static final String COLUMN_STATUS = "status";
    //VPlantable creation sql statement
    private static final String VPLAN_0_CREATE = "create table "
            + TABLE_VPLAN_0 + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_GRADE
            + " text, " + COLUMN_STUNDE + " text, "
            + COLUMN_STATUS + " text);";
    public static final String TABLE_LINKS = "linktable";
    public static final String COLUMN_TAG = "tag";
    public static final String COLUMN_URL = "url";
    //linktable creation sql statement
    private static final String LINK_CREATE = "create table "
            + TABLE_LINKS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_TAG
            + " text, " + COLUMN_URL + " text);";
    public static final String TABLE_ANNOUNCEMENTS = "announcements";
    public static final String COLUMN_ANNOUNCEMENT = "announcement";
    //announcement table
    public static final String ANNOUNCEMENTS_CREATE = "create table "
            + TABLE_ANNOUNCEMENTS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_ANNOUNCEMENT
            + " text);";
    public static final String TABLE_VPLAN_1 = "vplantable1";
    //VPlantable 1
    private static final String VPLAN_1_CREATE = "create table "
            + TABLE_VPLAN_1 + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_GRADE
            + " text, " + COLUMN_STUNDE + " text, "
            + COLUMN_STATUS + " text);";
    public static final String TABLE_VPLAN_2 = "vplantable2";
    //VPlantable 2
    private static final String VPLAN_2_CREATE = "create table "
            + TABLE_VPLAN_2 + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_GRADE
            + " text, " + COLUMN_STUNDE + " text, "
            + COLUMN_STATUS + " text);";
    public static final String TABLE_VPLAN_3 = "vplantable3";
    //VPlantable 3
    private static final String VPLAN_3_CREATE = "create table "
            + TABLE_VPLAN_3 + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_GRADE
            + " text, " + COLUMN_STUNDE + " text, "
            + COLUMN_STATUS + " text);";
    public static final String TABLE_VPLAN_4 = "vplantable4";
    //VPlantable 4
    private static final String VPLAN_4_CREATE = "create table "
            + TABLE_VPLAN_4 + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_GRADE
            + " text, " + COLUMN_STUNDE + " text, "
            + COLUMN_STATUS + " text);";
    public static final String[] tablesVplan = {TABLE_VPLAN_0, TABLE_VPLAN_1, TABLE_VPLAN_2, TABLE_VPLAN_3, TABLE_VPLAN_4};
    public static final String DATABASE_UINFO = "uinfo.db";
    public static final String DATABASE_MINFO = "minfo.db";
    public static final String DATABASE_OINFO = "oinfo.db";
    private static final int DATABASE_VERSION = 4;

    public SQLiteHelperVplan(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    /**
     * Creates all the tables needed in the vplan dbs
     *
     * @param database the database to contain the tables
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(VPLAN_0_CREATE);
        database.execSQL(VPLAN_1_CREATE);
        database.execSQL(VPLAN_2_CREATE);
        database.execSQL(VPLAN_3_CREATE);
        database.execSQL(VPLAN_4_CREATE);
        database.execSQL(LINK_CREATE);
        database.execSQL(ANNOUNCEMENTS_CREATE);
    }

    /**
     * Basically is just used to completely wipe all the tables in a db
     *
     * @param db
     * @param oldVersion not in use
     * @param newVersion not in use
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_0);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_1);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_2);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_3);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_4);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANNOUNCEMENTS);
        onCreate(db);
    }

    /**
     * Re-creates a single table in a db
     */
    public void newTable(SQLiteDatabase db, String tableName) {
        if (tableName.contentEquals(TABLE_VPLAN_0)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_0);
            db.execSQL(VPLAN_0_CREATE);
        }
        if (tableName.contentEquals(TABLE_VPLAN_1)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_1);
            db.execSQL(VPLAN_1_CREATE);
        }
        if (tableName.contentEquals(TABLE_VPLAN_2)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_2);
            db.execSQL(VPLAN_2_CREATE);
        }
        if (tableName.contentEquals(TABLE_VPLAN_3)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_3);
            db.execSQL(VPLAN_3_CREATE);
        }
        if (tableName.contentEquals(TABLE_VPLAN_4)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VPLAN_4);
            db.execSQL(VPLAN_4_CREATE);
        }
        if (tableName.contentEquals(TABLE_LINKS)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINKS);
            db.execSQL(LINK_CREATE);
        }
        if (tableName.contentEquals(TABLE_ANNOUNCEMENTS)) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANNOUNCEMENTS);
            db.execSQL(ANNOUNCEMENTS_CREATE);
        }
    }

} 