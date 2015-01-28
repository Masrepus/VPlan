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
    private SQLiteDatabase database;

    private SQLiteHelperVplan dbHelper;

    private Context context;

    /**
     * Creates instances of MySQLiteHelper for U/M/Oinfo
     *
     * @param context passed to MySQLiteHelper constructor
     */
    public VPlanDataSource(Context context) {
        this.context = context;
        dbHelper = new SQLiteHelperVplan(context, SQLiteHelperVplan.DB_NAME);
    }

    /**
     * Creates writable instances of all three dbs
     */
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Closes all dbs
     */
    public void close() {
        dbHelper.close();
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
        values.put(SQLiteHelperVplan.COLUMN_STUNDE, stunde);
        values.put(SQLiteHelperVplan.COLUMN_GRADE, klasse);
        values.put(SQLiteHelperVplan.COLUMN_STATUS, status);

        database.insert(tableName, null, values);
    }



    /**
     * Passes queries to the right database by checking which vplan mode is currently active
     *
     * @param tableName       the tablename
     * @param projection the columns to query for
     * @return a cursor object containing the queried columns
     */
    public Cursor query(String tableName, String[] projection) {

        return database.query(
                tableName,
                projection,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Passes requests to the right MySQLiteHelper object to re-create a specific table
     */
    public void newTable(String tableName) {

        dbHelper.newTable(database, tableName);
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
        if (c.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
