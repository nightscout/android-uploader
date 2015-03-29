package com.nightscout.android.events;

import android.app.ListFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private EventType currentFilter = EventType.ALL;
    private boolean menuInflated = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!menuInflated) {
            inflater.inflate(R.menu.menu_event_log, menu);
            menuInflated = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.clear_log):
                Log.d("XXX", "Clear log selected for " + currentFilter.name() + ". Now make it do stuff...");
                clear(currentFilter);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("menu_inflated", menuInflated);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (savedInstanceState != null && savedInstanceState.containsKey("menu_inflated")) {
            Log.d("XXX", "Appears to be coming back to life!");
            menuInflated = savedInstanceState.getBoolean("menu_inflated");
        }

        currentFilter = EventType.ALL;
        if (bundle != null && bundle.containsKey("Filter")) {
            currentFilter = EventType.values()[bundle.getInt("Filter")];
        }

        setListAdapter(getMessages(currentFilter));
        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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
//        db.close();
        return cursorAdapter;
    }

    public synchronized void clear(EventType type) {
        Log.d("AndroidEventReporter", "Clearing logs for: " + type.name());
        EventsDbHelper dbHelper = EventsDbHelper.getHelper(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (type == EventType.ALL) {
            db.delete(EventsContract.EventEntry.TABLE_NAME, null, null);
        } else {
            db.delete(EventsContract.EventEntry.TABLE_NAME, EventsContract.EventEntry.COLUMN_NAME_EVENT_TYPE + "=" + type.ordinal(), null);
        }
//        db.close();
    }

}
