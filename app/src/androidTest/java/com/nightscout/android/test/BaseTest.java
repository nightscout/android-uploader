package com.nightscout.android.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

public class BaseTest extends InstrumentationTestCase {
    @Override
    public void setUp() throws Exception {
        // make android create the cache dir, so that dexmaker can find one.
        // bug: https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache",
                getInstrumentation().getTargetContext().getCacheDir().getPath());

        getPreferences().edit().clear().commit();
    }

    protected Context getContext() {
        return getInstrumentation().getTargetContext();
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }
}
