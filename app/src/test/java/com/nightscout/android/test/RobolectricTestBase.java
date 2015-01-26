package com.nightscout.android.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.common.base.Function;
import com.nightscout.robolectric.RobolectricGradleRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleRunner.class)
public class RobolectricTestBase {
    private final boolean[] intentSeen = {false};

    @Before
    public final void setUpBase() {
        // NPEs happen when using Robolectric + GA for some reason. Disable them for now.
        // https://github.com/robolectric/robolectric/issues/1075
        getShadowApplication().declareActionUnbindable("com.google.android.gms.analytics.service.START");
        GoogleAnalytics.getInstance(getContext()).setAppOptOut(true);
    }

    public void whenOnBroadcastReceived(String intentKey, final Function<Intent, Void> verifyCallback) {
        getShadowApplication().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intentSeen[0] = true;
                verifyCallback.apply(intent);
            }
        }, new IntentFilter(intentKey));
    }

    public void assertIntentSeen() {
        assertThat(intentSeen[0], is(true));
    }

    public Context getContext() {
        return getShadowApplication().getApplicationContext();
    }

    public ShadowApplication getShadowApplication() {
        return Robolectric.getShadowApplication();
    }
}
