package com.nightscout.android.test;

import android.content.*;

import com.nightscout.android.BuildConfig;
import com.nightscout.android.CollectorService;
import com.nightscout.android.ProcessorService;
import net.tribe7.common.base.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml")
public class RobolectricTestBase {
    private final boolean[] intentSeen = {false};

    @Before
    public final void setUpBase() {
        getShadowApplication().declareActionUnbindable(
            "com.google.android.gms.analytics.service.START");
    }

    @Test
    public void shouldHaveApplication() {
        assertThat(getContext(), is(notNullValue()));
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
        return Shadows.shadowOf(RuntimeEnvironment.application);
    }
}
