package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by klee on 3/26/15.
 */
public class SyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("XXX", "Here!");
        if (intent.getAction().equals(CollectorService.ACTION_SYNC)) {
            int numOfPages = intent.getIntExtra(CollectorService.NUM_PAGES, 1);
            String syncType = intent.getStringExtra(CollectorService.SYNC_TYPE);
            Intent syncIntent = new Intent(context, CollectorService.class);
            syncIntent.setAction(CollectorService.ACTION_POLL);
            syncIntent.putExtra(CollectorService.NUM_PAGES, numOfPages);
            syncIntent.putExtra(CollectorService.SYNC_TYPE, syncType);
            context.startService(syncIntent);
        }
    }
}
