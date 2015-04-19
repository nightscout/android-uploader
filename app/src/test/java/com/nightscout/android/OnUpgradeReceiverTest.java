package com.nightscout.android;


import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import com.nightscout.android.test.RobolectricTestBase;
import com.nightscout.android.ui.MonitorFragment;
import com.nightscout.android.ui.NightscoutNavigationDrawer;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnUpgradeReceiverTest extends RobolectricTestBase {
    NightscoutNavigationDrawer activity;

    @Before
    public void setUp() {
//        ComponentName collectorComponentName = new ComponentName("com.nightscout.android", CollectorService.class);
        CollectorService.LocalBinder binder = mock(CollectorService.LocalBinder.class);
//        getShadowApplication().setComponentNameAndServiceForBindService(collectorComponentName, binder);
        activity = Robolectric.buildActivity(NightscoutNavigationDrawer.class).create().get();
        when(binder.getService()).thenReturn(mock(CollectorService.class));
        Shadows.shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getApplicationContext(), MonitorFragment.class), binder);
    }

    @Test
    public void testOnCreate_ShouldHaveUpgradeReceiver() {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_REPLACED);
        assertThat(getShadowApplication().hasReceiverForIntent(intent), is(true));
    }

    @Test
    public void testOnUpgradeReceiverRestartsMainActivity() {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_REPLACED);
        Uri dataUri = Uri.parse("package:com.nightscout.android");
        intent.setData(dataUri);
        UpgradeReceiver onUpgradeReceiver = new UpgradeReceiver();
        onUpgradeReceiver.onReceive(activity.getApplicationContext(), intent);
        Intent anIntent = getShadowApplication().getNextStartedActivity();
        assertThat(anIntent.getComponent().getClassName(), is(NightscoutNavigationDrawer.class.getName()));
    }

    //TODO: need a test to make sure that activity is not started if another package is replaced
}
