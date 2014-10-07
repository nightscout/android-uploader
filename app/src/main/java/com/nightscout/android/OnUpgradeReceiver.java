package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class OnUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context,final Intent intent)
    {
        final String msg="intent:"+intent+" action:"+intent.getAction();
        Intent mainActivity= new Intent(context,MainActivity.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mainActivity);
    }
}
