package com.barmpas.asthma.data;

import android.net.Uri;
import android.provider.BaseColumns;

/*
 * The contract class for the local SQLite Database class where the user stores the session.
 */

public final class SessionContract {

    public static final String CONTENT_AUTHORITY = "com.barmpas.asthma";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_SESSION = "session";

    private SessionContract() {
    }

    public static final class SessionEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_SESSION);
        public final static String TABLE_NAME = "sessions";
        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_BREATHES_IN = "bin";
        public final static String COLUMN_ERROR = "ain";
        public final static String COLUMN_ID = "id";
    }
}