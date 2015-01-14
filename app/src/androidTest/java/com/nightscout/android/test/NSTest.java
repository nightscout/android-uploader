package com.nightscout.android.test;

import android.test.ActivityInstrumentationTestCase2;

import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;


public class NSTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity activity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        activity = getActivity();
    }

    public NSTest(){
        super(MainActivity.class);
    }

    // Verifies the tracker returned by the application class is a singleton.
    public void testTracker(){
        Tracker tracker1=((Nightscout) getActivity().getApplicationContext()).getTracker();
        Tracker tracker2=((Nightscout) getActivity().getApplicationContext()).getTracker();
        assertEquals(tracker1,tracker2);
    }
}
