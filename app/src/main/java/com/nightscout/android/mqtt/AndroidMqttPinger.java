package com.nightscout.android.mqtt;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import com.nightscout.core.mqtt.Constants;
import com.nightscout.core.mqtt.MqttPinger;
import com.nightscout.core.mqtt.MqttPingerObservable;
import com.nightscout.core.mqtt.MqttPingerObserver;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

public class AndroidMqttPinger implements MqttPinger, MqttPingerObservable {
    private static final String TAG = AndroidMqttPinger.class.getSimpleName();
    private Context context;
    private MqttPingerReceiver pingerReceiver;
    private PendingIntent pingerPendingIntent;
    private int instanceId;
    private MqttClient mqttClient = null;
    private boolean active = false;
    private AlarmManager alarmMgr;
    private int keepAliveInterval;
    private String keepAliveTopic = "/users/%s/keepalive";
    private List<MqttPingerObserver> observers;

    public AndroidMqttPinger(Context context, int instanceId, MqttClient mqttClient, int keepAliveInterval) {
        this.context = context;
        this.instanceId = instanceId;
        this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.observers = new ArrayList<>();
        this.mqttClient = mqttClient;
        this.keepAliveInterval = keepAliveInterval;
    }

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void setKeepAliveTopic(String keepAliveTopic) {
        this.keepAliveTopic = keepAliveTopic;
    }

    @Override
    public void ping() {
        Log.i(TAG, "Ping");
        if (!isActive()) {
            Log.d(TAG, "Can't ping because connection is not active");
            return;
        }
        MqttMessage message = new MqttMessage(Constants.MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(Constants.MQTT_KEEP_ALIVE_QOS);
        try {
            mqttClient.publish(String.format(keepAliveTopic, mqttClient.getClientId()), message);
            Log.i(TAG, "Successful ping");
        } catch (MqttException e) {
            Log.wtf(TAG, "Exception during ping. Reason code:" + e.getReasonCode() + " Message: " + e.getMessage());
            for (MqttPingerObserver observer : observers) {
                observer.onFailedPing();
            }
        }
    }

    @Override
    public void start() {
        Log.i(TAG, "Starting ping");
        if (!isActive()) {
            pingerReceiver = new MqttPingerReceiver(this);
            context.registerReceiver(pingerReceiver, new IntentFilter(Constants.KEEPALIVE_INTENT_FILTER));
            active = true;
            reset();
            Log.d(TAG, "Pinger started");
        } else {
            Log.w(TAG, "Can't start pinger because it is already active");
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping ping");
        alarmMgr.cancel(pingerPendingIntent);
        if (isActive()) {
            context.unregisterReceiver(pingerReceiver);
            active = false;
            Log.d(TAG, "Pinger stopped");
        } else {
            Log.d(TAG, "Can't stop pinger because it is not active");
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setKeepAliveInterval(int ms) {
        keepAliveInterval = ms;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void reset() {
        if (!isActive()) {
            Log.d(TAG, "Can't reset pinger because it is not active");
            return;
        }
        alarmMgr.cancel(pingerPendingIntent);
        Log.d(TAG, "Setting next keep alive to trigger in " + (Constants.KEEPALIVE_INTERVAL - 3000) / 1000 + " seconds");
        Intent pingerIntent = new Intent(Constants.KEEPALIVE_INTENT_FILTER);
        pingerIntent.putExtra("device", instanceId);
        pingerPendingIntent = PendingIntent.getBroadcast(context, 61, pingerIntent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + keepAliveInterval - 3000, pingerPendingIntent);
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + keepAliveInterval - 3000, pingerPendingIntent);
        }

    }

    @Override
    public boolean isNetworkActive() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    @Override
    public void registerObserver(MqttPingerObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        } else {
            Log.d(TAG, "Observer already registered");
        }
    }

    @Override
    public void unregisterObserver(MqttPingerObserver observer) {
        if (!observers.contains(observer)) {
            observers.remove(observer);
        } else {
            Log.d(TAG, "Observer is not registered");
        }
    }

    // TODO(klee): honor disable background data setting..
}
