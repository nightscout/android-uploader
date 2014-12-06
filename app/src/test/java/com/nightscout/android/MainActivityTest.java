package com.nightscout.android;

import android.content.Intent;
import android.preference.PreferenceManager;

import com.google.common.collect.Lists;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MainActivityTest extends RobolectricTestBase {
    ActivityController<MainActivity> activityController;
    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(MainActivity.class);
    }

    @Test
    public void testOnCreate_ShouldConnectToastReceiver() {
        activityController.create().get();

        Intent toastIntent = ToastReceiver.createIntent(getContext(), R.string.unknown_mongo_host);
        assertThat(getShadowApplication().hasReceiverForIntent(toastIntent), is(true));
    }

    @Test
    public void testOnCreate_ShouldCleanUpOldStyleURIs() {
        NightscoutPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setRestApiBaseUris(Lists.newArrayList("abc@http://example.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(1));
        assertThat(preferences.getRestApiBaseUris(), containsInAnyOrder("http://abc@example.com"));
    }

    @Test
    public void testOnCreate_ShouldCleanUpOldStyleUris_Multiple() {
        NightscoutPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setRestApiBaseUris(Lists.newArrayList("abc@http://example.com", "http://example2.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(2));
        assertThat(preferences.getRestApiBaseUris(), containsInAnyOrder(
                "http://abc@example.com", "http://example2.com"));
    }
}
