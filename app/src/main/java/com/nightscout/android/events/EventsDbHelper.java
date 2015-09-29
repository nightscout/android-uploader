package com.nightscout.android.events;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EventsDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Nightscout.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + EventsContract.EventEntry.TABLE_NAME + " (" +
                    EventsContract.EventEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP + " INTEGER NOT NULL, " +
                    EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE + " INTEGER NOT NULL, " +
                    EventsContract.EventEntry.COLUMN_NAME_SEVERITY + " STRING NOT NULL, " +
                    EventsContract.EventEntry.COLUMN_NAME_MESSAGE + " TEXT NOT NULL" +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + EventsContract.EventEntry.TABLE_NAME;

    private static EventsDbHelper instance;

    public static synchronized EventsDbHelper getHelper(Context context) {
        if (instance == null) {
            instance = new EventsDbHelper(context);
        }
        return instance;
    }

    private EventsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
