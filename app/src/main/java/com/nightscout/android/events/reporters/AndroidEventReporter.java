package com.nightscout.android.events.reporters;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.nightscout.android.events.EventsContract;
import com.nightscout.android.events.EventsDbHelper;
import com.nightscout.core.events.reporters.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import java.util.Date;


public class AndroidEventReporter implements EventReporter {
    private SQLiteDatabase db;
    private EventsDbHelper dbHelper;
    private static AndroidEventReporter instance;

    private AndroidEventReporter(Context context) {
        this.dbHelper = EventsDbHelper.getHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }

    public static AndroidEventReporter getReporter(Context context) {
        if (instance == null) {
            instance = new AndroidEventReporter(context);
        }
        return instance;
    }

    public synchronized void report(EventType type, EventSeverity severity, String message) {
        Log.d("AndroidEventReporter", type.name() + " " + severity.name() + " " + message);
        ContentValues values = new ContentValues();
        values.put(EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP, new Date().getTime());
        values.put(EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE, type.ordinal());
        values.put(EventsContract.EventEntry.COLUMN_NAME_SEVERITY, severity.name());
        values.put(EventsContract.EventEntry.COLUMN_NAME_MESSAGE, message);
        long result = db.insert(EventsContract.EventEntry.TABLE_NAME, null, values);
    }

}
