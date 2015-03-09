package com.nightscout.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.model.GlucoseUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ActivityController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MainActivityTest extends RobolectricTestBase {
    ActivityController<MainActivity> activityController;
    AndroidPreferences preferences;

    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(MainActivity.class);
        preferences = new AndroidPreferences(getContext());
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnCreate_ShouldConnectToastReceiver() {
        activityController.create().get();
        Intent toastIntent = ToastReceiver.createIntent(getContext(), R.string.unknown_mongo_host);
        assertThat(getShadowApplication().hasReceiverForIntent(toastIntent), is(true));
    }

    @Test
    public void testOnCreate_ShouldCleanUpOldStyleURIs() {
        preferences.setRestApiBaseUris(Lists.newArrayList("abc@http://example.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(1));
        assertThat(preferences.getRestApiBaseUris(), containsInAnyOrder("http://abc@example.com"));
    }

    @Test
    public void testOnCreate_ShouldCleanUpOldStyleUris_Multiple() {
        preferences.setRestApiBaseUris(Lists.newArrayList("abc@http://example.com", "http://example2.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(2));
        assertThat(preferences.getRestApiBaseUris(), containsInAnyOrder(
                "http://abc@example.com", "http://example2.com"));
    }

    @Test
    public void testOnCreate_ShouldShowIUnderstandDialog() {
        preferences.setIUnderstand(false);
        // make sure we don't have another alert dialog
        preferences.setAskedForData(true);

        activityController.create().get();
        ShadowAlertDialog alertDialog = getShadowApplication().getLatestAlertDialog();
        assertThat(alertDialog, is(not(nullValue())));
        assertThat(alertDialog.getTitle(),
                is(getContext().getText(R.string.pref_title_i_understand)));
    }


    @Test
    public void testOnCreate_ShouldNotShowIUnderstandDialogIfAlreadySet() {
        preferences.setIUnderstand(true);
        // make sure we don't have another alert dialog
        preferences.setAskedForData(true);

        activityController.create().get();
        ShadowAlertDialog alertDialog = getShadowApplication().getLatestAlertDialog();
        assertThat(alertDialog, is(nullValue()));
    }

    @Test
    public void testOnCreate_ShouldClearOutInvalidMongoUri() {
        preferences.setMongoClientUri("invalid/db");
        activityController.create().get();
        assertThat(preferences.getMongoClientUri(), isEmptyString());
    }

    @Test
    public void testOnCreate_ShouldClearOutInvalidRestUris() {
        preferences.setRestApiBaseUris(Lists.newArrayList("\\invalid"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), is(empty()));
    }

    @Test
    public void testOnCreate_ShouldPartiallyClearOutInvalidRestUris() {
        preferences.setRestApiBaseUris(Lists.newArrayList("\\invalid", "http://example.com"));
        activityController.create().get();
        assertThat(preferences.getRestApiBaseUris(), hasSize(1));
        assertThat(preferences.getRestApiBaseUris(), hasItem(is("http://example.com")));
    }

    @Test
    public void testOnResume_ShouldCallPebbleConfig() {
        activityController.create().start();
        Activity activity = activityController.get();
        Pebble pebble = mock(Pebble.class);
        ((MainActivity) activity).setPebble(pebble);
        activityController.stop().resume();
        verify(pebble, times(1)).config(anyString(), (GlucoseUnit) anyObject(), (Context) anyObject());
    }

    @Test
    public void testOnResume_SavedSgViewShouldBeRestored() {
        activityController.create().start();
        Activity activity = activityController.get();
        TextView sgView = (TextView) activity.findViewById(R.id.sgValue);
        sgView.setTag(R.string.display_sgv, 100);
        sgView.setTag(R.string.display_trend, 4);
        activityController.resume();
        assertThat(sgView.getText().toString(), is("100 " + TrendArrow.FLAT.symbol()));
    }

    @Test
    public void testOnResume_SavedSgViewSpecialValueShouldBeRestored() {
        activityController.create().start();
        Activity activity = activityController.get();
        TextView sgView = (TextView) activity.findViewById(R.id.sgValue);
        sgView.setTag(R.string.display_sgv, 5);
        sgView.setTag(R.string.display_trend, 0);
        activityController.resume();
        assertThat(sgView.getText().toString(), is("?NC"));
    }

    @Test
    public void testOnResume_SavedSgViewNoReadingShouldBeRestored() {
        activityController.create().start();
        Activity activity = activityController.get();
        TextView sgView = (TextView) activity.findViewById(R.id.sgValue);
        sgView.setTag(R.string.display_sgv, -1);
        sgView.setTag(R.string.display_trend, 0);
        activityController.resume();
        assertThat(sgView.getText().toString(), is("---"));
    }
}
