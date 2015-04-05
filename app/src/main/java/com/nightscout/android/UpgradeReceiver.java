package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nightscout.android.ui.NightscoutNavigationDrawer;

public class UpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getDataString().contains("com.nightscout.android")) {
            Intent mainActivity = new Intent(context, NightscoutNavigationDrawer.class);
            mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivity);
        }
    }
}
