package com.nightscout.android.mqtt;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.google.common.collect.Lists;
import com.nightscout.core.mqtt.Constants;
import com.nightscout.core.mqtt.MqttTimer;
import com.nightscout.core.mqtt.MqttTimerObserver;

import java.util.Date;
import java.util.List;

public class AndroidMqttTimer implements MqttTimer {
    private static final String TAG = AndroidMqttTimer.class.getSimpleName();
    private List<MqttTimerObserver> observers = Lists.newArrayList();
    private MqttTimerReceiver timerReceiver;
    private AlarmManager alarmMgr;
    private boolean active = false;
    private Context context;
    Intent alarmIntent;
    PendingIntent pendingAlarmIntent;
    private int Id;

    public AndroidMqttTimer(Context context, int Id) {
        this.context = context;
        this.Id = Id;
        this.timerReceiver = new MqttTimerReceiver(observers);
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d(TAG, "Creating timer ID#" + Id);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void setTimer(long delayMs) {
        cancel();
        alarmIntent = new Intent(Constants.RECONNECT_INTENT_FILTER);
        alarmIntent.putExtra("device", Id);
        pendingAlarmIntent = PendingIntent.getBroadcast(context, 61 + Id, alarmIntent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "Setting KitKat alarm");
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, new Date().getTime() + delayMs, pendingAlarmIntent);
        } else {
            Log.d(TAG, "Setting pre-KitKat alarm");
            alarmMgr.set(AlarmManager.RTC_WAKEUP, new Date().getTime() + delayMs, pendingAlarmIntent);
        }
        Log.d(TAG, "Created a timer with a delay of " + delayMs + " ms");
    }

    @Override
    public void cancel() {
        Log.d(TAG, "Canceling timer");
        alarmMgr.cancel(pendingAlarmIntent);
    }

    @Override
    public void activate() {
        if (!active) {
            context.registerReceiver(timerReceiver, new IntentFilter(Constants.RECONNECT_INTENT_FILTER));
            active = true;
            Log.i(TAG, "Reconnect timer activiated");
        } else {
            Log.w(TAG, "Timer already activiated");
        }
    }

    @Override
    public void deactivate() {
        if (active) {
            context.unregisterReceiver(timerReceiver);
            active = false;
            Log.i(TAG, "Reconnect timer deactiviated");
        } else {
            Log.w(TAG, "Timer already deactiviated");
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void registerObserver(MqttTimerObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            Log.w(TAG, "Observer registered");
        } else {
            Log.w(TAG, "Observer already registered");
        }
    }

    @Override
    public void unregisterObserver(MqttTimerObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
            Log.w(TAG, "Observer unregistered");
        } else {
            Log.w(TAG, "Observer not registered");
        }
    }
}
