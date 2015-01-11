package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Listens to Intents from non-UI threads, and shows a Toast for messages from them.
 */
public class ToastReceiver extends BroadcastReceiver {
    public static final String ACTION_SEND_NOTIFICATION = "nightscout_toast_intent";
    public static final String TOAST_MESSAGE = "nightscout_toast_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_SEND_NOTIFICATION)) {
            Toast.makeText(context, intent.getStringExtra(TOAST_MESSAGE), Toast.LENGTH_LONG).show();
        }
    }

    public static Intent createIntent(Context context, int localizedErrorMessage) {
        Intent intent = new Intent(ACTION_SEND_NOTIFICATION);
        intent.putExtra(TOAST_MESSAGE, context.getString(localizedErrorMessage));
        return intent;
    }
}
