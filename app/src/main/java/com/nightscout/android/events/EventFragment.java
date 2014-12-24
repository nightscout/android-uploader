package com.nightscout.android.events;

import android.app.ListFragment;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nightscout.android.R;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import java.util.List;

public class EventFragment extends ListFragment {
    List<String> messages;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        EventType type = EventType.values()[bundle.getInt("Filter")];
        setListAdapter(getMessages(type));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private CursorAdapter getMessages(EventType type) {
        EventsDbHelper dbHelper = EventsDbHelper.getHelper(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = new String[]{EventsContract.EventEntry._ID,
                EventsContract.EventEntry.COLUMN_NAME_MESSAGE,
                EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP,
                EventsContract.EventEntry.COLUMN_NAME_SEVERITY
        };
        Cursor cursor = db.query(
                EventsContract.EventEntry.TABLE_NAME,
                projection,
                String.format("%s=?", EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE),
                new String[]{String.valueOf(type.ordinal())},
                null,
                null,
                EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP + " DESC",
                "100"
        );
        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.event_list_view, cursor,
                new String[]{EventsContract.EventEntry.COLUMN_NAME_MESSAGE,
                        EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP
//                        EventsContract.EventEntry.COLUMN_NAME_SEVERITY
                },
//                new int[]{R.id.event_message, R.id.event_timestamp, R.id.event_severity},
                new int[]{R.id.event_message, R.id.event_timestamp},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                String severity = cursor.getString(
                        cursor.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_SEVERITY));
                EventSeverity sev = EventSeverity.valueOf(severity);
                if (sev == EventSeverity.WARN) {
                    ((TextView) view).setTextColor(Color.YELLOW);
                }
                if (sev == EventSeverity.ERROR) {
                    ((TextView) view).setTextColor(Color.RED);
                }
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            }
        });
        return cursorAdapter;
    }
}
