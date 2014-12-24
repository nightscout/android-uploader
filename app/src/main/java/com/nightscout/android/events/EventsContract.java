package com.nightscout.android.events;

import android.provider.BaseColumns;

public final class EventsContract {

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public EventsContract() {
    }

    public static abstract class EventEntry implements BaseColumns {
        public static final String TABLE_NAME = "events";
        public static final String COLUMN_NAME_TIME_STAMP = "timestamp";
        public static final String COLUMN_NAME_EVENT_TYPE = "type";
        public static final String COLUMN_NAME_SEVERITY = "severity";
        public static final String COLUMN_NAME_MESSAGE = "message";
    }
}
