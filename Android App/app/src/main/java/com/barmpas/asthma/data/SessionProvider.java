package com.barmpas.asthma.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/*
 * The provider for the local SQLite Database class where the user stores session.
 * This class allows the interaction between the main activities and the SQLite Database.
 */

public class SessionProvider extends ContentProvider {

    //Define SQL versions

    private SessionDbHelper mDbHelper;
    static SQLiteDatabase generalDB;

    public static final int SESSION = 100;
    public static final int SESSION_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(SessionContract.CONTENT_AUTHORITY, SessionContract.PATH_SESSION, SESSION);
        sUriMatcher.addURI(SessionContract.CONTENT_AUTHORITY, SessionContract.PATH_SESSION + "/#", SESSION_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SessionDbHelper(getContext());
        generalDB = mDbHelper.getReadableDatabase();
        return true;
    }

    //Using cursor traverse the sqlite database

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case SESSION:
                cursor = database.query(SessionContract.SessionEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case SESSION_ID:
                selection = SessionContract.SessionEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(SessionContract.SessionEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    //Using cursor insert a new element to the SQLite Database

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SESSION:
                Uri newUri = insertSession(uri, contentValues);
                getContext().getContentResolver().notifyChange(uri, null);
                return newUri;
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertSession(Uri uri, ContentValues values) {
        Integer ido = values.getAsInteger(SessionContract.SessionEntry.COLUMN_ID);

        if (ido == null) {
            throw new IllegalArgumentException("invalid");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(SessionContract.SessionEntry.TABLE_NAME, null, values);
        if (id == -1) {
            return null;
        }
        return ContentUris.withAppendedId(uri, id);
    }

    //Delete the whole SQLite Database

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;
        switch (match) {
            case SESSION:
                rowsUpdated = database.delete(SessionContract.SessionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case SESSION_ID:
                selection = SessionContract.SessionEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = database.delete(SessionContract.SessionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not successful for " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        return 0;
    }

}