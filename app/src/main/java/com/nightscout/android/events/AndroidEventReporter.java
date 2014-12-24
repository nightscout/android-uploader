package com.nightscout.android.events;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;


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

    public void report(EventType type, EventSeverity severity, String message) {
        ContentValues values = new ContentValues();
        values.put(EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP,
                new DateTime().toString(DateTimeFormat.forPattern("MM-dd HH:mm Z")));
        values.put(EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE, type.ordinal());
        values.put(EventsContract.EventEntry.COLUMN_NAME_SEVERITY, severity.name());
        values.put(EventsContract.EventEntry.COLUMN_NAME_MESSAGE, message);
        long result = db.insert(EventsContract.EventEntry.TABLE_NAME, null, values);
    }
}
