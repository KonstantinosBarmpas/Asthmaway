package com.barmpas.asthma.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * The helper function class for the local SQLite Database class where the user stores the session.
 * Here we define the variables and the acceptable form of the entries in the database.
 */

public class SessionDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "session.db";
    private static final int DATABASE_VERSION = 1;

    public SessionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_TABLE = "CREATE TABLE " + SessionContract.SessionEntry.TABLE_NAME + " ("
                + SessionContract.SessionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SessionContract.SessionEntry.COLUMN_BREATHES_IN + " TEXT NOT NULL , "
                + SessionContract.SessionEntry.COLUMN_ERROR + " TEXT NOT NULL , "
                + SessionContract.SessionEntry.COLUMN_ID + " LONG NOT NULL);";
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Still at version 1, no upgrade required
    }
}
