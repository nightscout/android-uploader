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
import org.robolectric.shadows.ShadowAlertDialog;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
    
    @Test
    public void testOnCreate_ShouldShowIUnderstandDialog() {
        AndroidPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setIUnderstand(false);
        // make sure we don't have another alert dialog
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("donate_data_query", true).apply();

        activityController.create().get();
        ShadowAlertDialog alertDialog = getShadowApplication().getLatestAlertDialog();
        assertThat(alertDialog, is(not(nullValue())));
        assertThat(alertDialog.getTitle(),
                is(getContext().getText(R.string.pref_title_i_understand)));
    }


    @Test
    public void testOnCreate_ShouldNotShowIUnderstandDialogIfAlreadySet() {
        AndroidPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setIUnderstand(true);
        // make sure we don't have another alert dialog
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("donate_data_query", true).apply();

        activityController.create().get();
        ShadowAlertDialog alertDialog = getShadowApplication().getLatestAlertDialog();
        assertThat(alertDialog, is(nullValue()));
    }

    @Test
    public void testOnCreate_ShouldClearOutInvalidMongoUri() {
        AndroidPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setMongoClientUri("invalid/db");
        activityController.create().get();
        assertThat(preferences.getMongoClientUri(), isEmptyString());
    }

    @Test
    public void testOnCreate_ShouldClearOutInvalidRestUris() {
        AndroidPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setRestApiBaseUris(Lists.newArrayList("\\invalid"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), is(empty()));
    }

    @Test
    public void testOnCreate_ShouldPartiallyClearOutInvalidRestUris() {
        AndroidPreferences preferences = new AndroidPreferences(PreferenceManager.getDefaultSharedPreferences(getContext()));
        preferences.setRestApiBaseUris(Lists.newArrayList("\\invalid", "http://example.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(1));
        assertThat(preferences.getRestApiBaseUris(), hasItem(is("http://example.com")));
    }
}
