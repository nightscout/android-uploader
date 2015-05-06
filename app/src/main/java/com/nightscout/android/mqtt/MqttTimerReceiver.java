package com.nightscout.android.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nightscout.core.mqtt.Constants;
import com.nightscout.core.mqtt.MqttTimerObserver;

import java.util.List;

public class MqttTimerReceiver extends BroadcastReceiver {
    private static final String TAG = MqttTimerReceiver.class.getSimpleName();
    private List<MqttTimerObserver> observers;

    public MqttTimerReceiver(List<MqttTimerObserver> observers) {
        this.observers = observers;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction().equals(Constants.RECONNECT_INTENT_FILTER)) {
            Log.d(TAG, "Received broadcast to that time is up. Observers: " + observers.size());
            for (MqttTimerObserver observer : observers) {
                observer.timerUp();
            }
//        }
    }
}