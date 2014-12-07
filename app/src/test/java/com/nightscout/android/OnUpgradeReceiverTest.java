package com.nightscout.android;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.nightscout.android.test.RobolectricTestBase;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OnUpgradeReceiverTest extends RobolectricTestBase {
    MainActivity activity;

    @Before
    public void setUp() {
        activity.notTesting = false;
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
//        SharedPreferences internal = activity.getApplicationContext().getSharedPreferences("showcase_internal", Context.MODE_PRIVATE);
//        internal.edit().putBoolean("hasShot" + 1, true).apply();
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
        OnUpgradeReceiver onUpgradeReceiver = new OnUpgradeReceiver();
        onUpgradeReceiver.onReceive(activity.getApplicationContext(),intent);
        Intent anIntent = getShadowApplication().getNextStartedActivity();
        assertThat(anIntent.getComponent().getClassName(), is(MainActivity.class.getName()));
    }

    //TODO: need a test to make sure that activity is not started if another package is replaced
}
