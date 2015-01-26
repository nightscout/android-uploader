package com.nightscout.android.wearables;

import android.content.Intent;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.nightscout.android.MainActivity;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.dexcom.TrendArrow;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WearableTest extends RobolectricTestBase {
    MainActivity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
    }

    @Test
    public void pebbleShouldCreateDataReceiver() {
        Pebble pebble = new Pebble(activity.getApplicationContext());
        Intent pebbleIntent = new Intent("com.getpebble.action.app.RECEIVE");
        assertThat(getShadowApplication().hasReceiverForIntent(pebbleIntent), is(true));
    }

    private PebbleDictionary createMockPebbleDictionary() {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addString(0, String.valueOf(TrendArrow.FLAT.ordinal()));
        dict.addString(1, "100");
        dict.addUint32(2, 1417990743);
        dict.addUint32(3, 1417990743);
        dict.addString(4, "0");
        dict.addString(5, "100");
        dict.addString(6, "Bob");
        return dict;
    }

}
