package com.nightscout.core.mqtt;

import com.google.common.collect.Lists;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.events.EventReporter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

public class MqttEventMgr implements MqttCallback, MqttPingerObserver, MqttMgrObservable,
        MqttTimerObserver {
    private final static String TAG = MqttEventMgr.class.getSimpleName();
    private List<MqttMgrObserver> observers = Lists.newArrayList();
    private MqttClient client;
    private MqttConnectOptions options;
    private MqttTimer timer;
    private MqttPinger pinger;
    private long reconnectDelay = 10000L;
    private int defaultQOS = Constants.QOS_2;
    private EventReporter reporter;
    private MqttConnectionState state = MqttConnectionState.DISCONNECTED;
    private ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
            Locale.getDefault());

    public MqttEventMgr(MqttClient client, MqttConnectOptions options, MqttPinger pinger,
                        MqttTimer timer, EventReporter reporter) {
        this.client = checkNotNull(client);
        this.options = checkNotNull(options);
        this.timer = checkNotNull(timer);
        this.pinger = checkNotNull(pinger);
        this.client.setCallback(MqttEventMgr.this);
        this.reporter = reporter;
    }

    public void connect() {
        try {
            client.connect(options);
            reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                    messages.getString("mqtt_connected"));
            if (!pinger.isActive()) {
                pinger.registerObserver(this);
                pinger.start();
            }
            state = MqttConnectionState.CONNECTED;
        } catch (MqttSecurityException e) {
            // TODO: Determine how to notify user
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_invalid_credentials"));
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
        ;
        if (!timer.isActive()) {
            timer.registerObserver(this);
            timer.activate();
        }
        timer.setTimer(delayMs);
        state = MqttConnectionState.RECONNECTING;
    }

    public void reconnect() {
        if (pinger.isNetworkActive()) {
            disconnect();
            connect();
        } else {
            // TODO: Figure out what to do here
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_reconnect_fail"));
        }
    }

    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                        messages.getString("mqtt_disconnected"));
            }
            timer.deactivate();
            pinger.stop();
        } catch (MqttException e) {
            // TODO: determine how to handle this
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_disconnect_fail"));
        }
        state = MqttConnectionState.DISCONNECTED;
    }

    public void close() {
        try {
            disconnect();
            client.close();
            reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                    messages.getString("mqtt_close"));
        } catch (MqttException e) {
            // TODO: determine how to handle this
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_close_fail"));
        }
        pinger.unregisterObserver(this);
        timer.unregisterObserver(this);
    }


    @Override
    public void onFailedPing() {
        reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                messages.getString("mqtt_ping_fail"));
//        notifyOnDisconnect();
//        delayedReconnect();
    }

    @Override
    public void timerUp() {
        reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                messages.getString("mqtt_reconnect"));
        reconnect();
    }

    // Test Everything after this
    @Override
    public void connectionLost(Throwable cause) {
        if (state != MqttConnectionState.CONNECTED) {
            return;
        }
        reporter.report(EventType.UPLOADER, EventSeverity.WARN,
                messages.getString("mqtt_lost_connection"));
        notifyOnDisconnect();
        delayedReconnect();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public MqttClient getClient() {
        return client;
    }

    public MqttConnectionState getState() {
        return state;
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
        boolean willReconnect = false;
        for (String topic : mqTopics) {
            try {
                client.subscribe(topic, QOS);
            } catch (MqttException e) {
                // TODO: Determine what to do here
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        messages.getString("mqtt_subscribe_fail"));
                // FIXME: a bit heavy handed here. Need to inspect error code to determine correct
                // actions
                if (!willReconnect) {
                    this.delayedReconnect();
                    willReconnect = true;
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
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_publish_fail"));
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
                // TODO: Determine what to do here. Should send message to acra for further
                // analysis
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        messages.getString("mqtt_observer_fail"));
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
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        messages.getString("mqtt_observer_fail"));
            }
        }
    }

    public MqttConnectOptions getOptions() {
        return options;
    }
}
