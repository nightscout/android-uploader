package com.nightscout.android.mqtt;

import com.google.common.collect.Lists;
import com.nightscout.core.mqtt.Constants;
import com.nightscout.core.mqtt.MqttMgrObservable;
import com.nightscout.core.mqtt.MqttMgrObserver;
import com.nightscout.core.mqtt.MqttPinger;
import com.nightscout.core.mqtt.MqttPingerObserver;
import com.nightscout.core.mqtt.MqttTimer;
import com.nightscout.core.mqtt.MqttTimerObserver;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MqttEventMgr implements MqttCallback, MqttPingerObserver, MqttMgrObservable, MqttTimerObserver {
    private final static String TAG = MqttEventMgr.class.getSimpleName();
    private List<MqttMgrObserver> observers = Lists.newArrayList();
    private MqttClient client;
    private MqttConnectOptions options;
    private MqttTimer timer;
    private MqttPinger pinger;
    private long reconnectDelay = 10000L;
    private int defaultQOS = Constants.QOS_2;

    public MqttEventMgr(MqttClient client, MqttConnectOptions options, MqttPinger pinger, MqttTimer timer) {
        this.client = checkNotNull(client);
        this.options = checkNotNull(options);
        this.timer = checkNotNull(timer);
        this.pinger = checkNotNull(pinger);
//        Log.i(TAG, "Setting up MqttEventManager");
        this.client.setCallback(MqttEventMgr.this);
    }

    public void connect() {
        try {
            client.connect(options);
//            Log.i(TAG, "MQTT Connected");
            if (!pinger.isActive()) {
                pinger.registerObserver(this);
                pinger.start();
            }
        } catch (MqttSecurityException e) {
            // TODO: Determine how to notify user
            if (timer.isActive()) {
                timer.deactivate();
            }
            if (pinger.isActive()) {
                pinger.stop();
            }
        } catch (MqttException e) {
            delayedReconnect();
        }
    }

    public void delayedReconnect() {
        delayedReconnect(reconnectDelay);
    }

    public void delayedReconnect(long delayMs) {
        if (!timer.isActive()) {
            timer.registerObserver(this);
            timer.activate();
        }
        timer.setTimer(delayMs);
    }

    public void reconnect() {
        if (pinger.isNetworkActive()) {
            disconnect();
            connect();
        } else {
            // TODO: Figure out what to do here
        }
    }

    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            timer.deactivate();
            pinger.stop();
        } catch (MqttException e) {
            // TODO: determine how to handle this
        }
    }

    public void close() {
        try {
            disconnect();
            client.close();
        } catch (MqttException e) {
            // TODO: determine how to handle this
        }
        pinger.unregisterObserver(this);
        timer.unregisterObserver(this);
    }


    @Override
    public void onFailedPing() {
        notifyOnDisconnect();
        delayedReconnect();
    }

    @Override
    public void timerUp() {
        reconnect();
    }

    // Test Everything after this
    @Override
    public void connectionLost(Throwable cause) {
//        Log.i(TAG, "Lost connection. Attempting to reconnect");
        notifyOnDisconnect();
        delayedReconnect();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public MqttClient getClient() {
        return client;
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        if (!mqttMessage.isDuplicate()) {
            notifyOnMessage(topic, mqttMessage);
        }
        pinger.reset();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        pinger.reset();
    }

    public void setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public void setDefaultQOS(int defaultQOS) {
        this.defaultQOS = defaultQOS;
    }

    public void subscribe(String... topics) {
        subscribe(defaultQOS, topics);
    }

    public void subscribe(int QOS, String... topics) {
        List<String> mqTopics = Lists.newArrayList(topics);
        boolean isReconnecting = false;
        for (String topic : mqTopics) {
            try {
                client.subscribe(topic, QOS);
            } catch (MqttException e) {
                // TODO: Determine what to do here
                // FIXME: a bit heavy handed here. Need to inspect error code to determine correct
                // actions
                if (!isReconnecting) {
                    this.delayedReconnect();
                    isReconnecting = true;
                }
            }
        }
    }

    public void publish(byte[] message, String topic) {
        publish(message, topic, defaultQOS);
    }

    public void publish(byte[] message, String topic, int QOS) {
        try {
            client.publish(topic, message, QOS, true);
        } catch (MqttException e) {
            // TODO: Determine what to do here
            this.delayedReconnect();
        }
    }

    @Override
    public void registerObserver(MqttMgrObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterObserver(MqttMgrObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    public int getNumberOfObservers() {
        return observers.size();
    }

    @Override
    public void notifyOnMessage(String topic, MqttMessage message) {
        for (MqttMgrObserver observer : observers) {
            try {
                observer.onMessage(topic, message);
            } catch (Exception e) {
                // Horrible catch all but I don't want the manager to die due to an unhandled
                // exception from one of the observers.
                // TODO: Determine what to do here
            }
        }
    }

    @Override
    public void notifyOnDisconnect() {
        for (MqttMgrObserver observer : observers) {
            try {
                observer.onDisconnect();
            } catch (Exception e) {
                // Horrible catch all but I don't want the manager to die due to an unhandled
                // exception from one of the observers.
                // TODO: Determine what to do here
            }
        }
    }

    public MqttConnectOptions getOptions() {
        return options;
    }
}
