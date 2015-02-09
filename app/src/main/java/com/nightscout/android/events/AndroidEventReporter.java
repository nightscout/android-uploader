package com.nightscout.android.events;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import java.util.Date;

public class AndroidEventReporter implements EventReporter {

  private SQLiteDatabase db;

  private AndroidEventReporter(Context context) {
    this.db = EventsDbHelper.getHelper(context).getWritableDatabase();
  }

  public static AndroidEventReporter getReporter(Context context) {
    return new AndroidEventReporter(context);
  }

  public synchronized void report(EventType type, EventSeverity severity, String message) {
    Log.d("AndroidEventReporter", type.name() + " " + severity.name() + " " + message);
    ContentValues values = new ContentValues();
    values.put(EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP, new Date().getTime());
    values.put(EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE, type.ordinal());
    values.put(EventsContract.EventEntry.COLUMN_NAME_SEVERITY, severity.name());
    values.put(EventsContract.EventEntry.COLUMN_NAME_MESSAGE, message);
    db.insert(EventsContract.EventEntry.TABLE_NAME, null, values);
  }
}
