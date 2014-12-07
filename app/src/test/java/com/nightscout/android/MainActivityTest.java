package com.nightscout.android;

import android.content.Intent;

import com.nightscout.android.test.RobolectricTestBase;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MainActivityTest extends RobolectricTestBase {
    MainActivity activity;
    @Before
    public void setUp() {
        activity.notTesting = false;
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
    }

    @Test
    public void testOnCreate_ShouldConnectToastReceiver() {
        Intent toastIntent = ToastReceiver.createIntent(getContext(), R.string.unknown_mongo_host);
        assertThat(getShadowApplication().hasReceiverForIntent(toastIntent), is(true));
    }
}
