package com.nightscout.android.events;

import android.app.ListFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nightscout.android.R;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class EventFragment extends ListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        EventType type = EventType.ALL;
        if (bundle != null && bundle.containsKey("Filter")) {
            type = EventType.values()[bundle.getInt("Filter")];
        }
        setListAdapter(getMessages(type));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static EventFragment newDeviceLogPanel() {
        EventFragment fragment = new EventFragment();
        Bundle args = new Bundle();
        args.putInt("Filter", EventType.DEVICE.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    public static EventFragment newUploadLogPanel() {
        EventFragment fragment = new EventFragment();
        Bundle args = new Bundle();
        args.putInt("Filter", EventType.UPLOADER.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    public static EventFragment newAllLogPanel() {
        EventFragment fragment = new EventFragment();
        Bundle args = new Bundle();
        args.putInt("Filter", EventType.ALL.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    private CursorAdapter getMessages(EventType type) {
        EventsDbHelper dbHelper = EventsDbHelper.getHelper(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = new String[]{EventsContract.EventEntry._ID,
                EventsContract.EventEntry.COLUMN_NAME_MESSAGE,
                EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP,
                EventsContract.EventEntry.COLUMN_NAME_SEVERITY
        };
        String selectionArgs[] = (type == EventType.ALL) ?
                null : new String[]{String.valueOf(type.ordinal())};
        String selection = (type == EventType.ALL) ?
                null : String.format("%s=?", EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE);
        Cursor cursor = db.query(
                EventsContract.EventEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
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
                },
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
                if (cursor.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_TIME_STAMP) == columnIndex) {
                    ((TextView) view).setText(new DateTime(cursor.getLong(columnIndex)).toString(DateTimeFormat.forPattern("MM-dd HH:mm")));
                } else {
                    ((TextView) view).setText(cursor.getString(columnIndex));

                }
                return true;
            }
        });
        return cursorAdapter;
    }
}
