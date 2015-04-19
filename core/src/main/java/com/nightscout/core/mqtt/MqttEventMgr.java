package com.nightscout.core.mqtt;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static net.tribe7.common.base.Preconditions.checkNotNull;

public class MqttEventMgr implements MqttCallback, MqttPingerObserver, MqttMgrObservable,
        MqttTimerObserver {
    protected final Logger log = LoggerFactory.getLogger(MqttEventMgr.class);
    private final static String TAG = MqttEventMgr.class.getSimpleName();
    private List<MqttMgrObserver> observers = new ArrayList<>();
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
    private boolean shouldReconnect = false;

    public MqttEventMgr(MqttClient client, MqttConnectOptions options, MqttPinger pinger,
                        MqttTimer timer, EventReporter reporter) {
        this.client = checkNotNull(client);
        this.options = checkNotNull(options);
        this.timer = checkNotNull(timer);
        this.pinger = checkNotNull(pinger);
        this.client.setCallback(MqttEventMgr.this);
        this.reporter = reporter;
    }

    public void setShouldReconnect(boolean shouldReconnect) {
        this.shouldReconnect = shouldReconnect;
    }

    public void connect() {
        try {
            log.info("MQTT connect issued");
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
            log.info("MQTT security issue");
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_invalid_credentials"));
            if (timer.isActive()) {
                timer.deactivate();
            }
            if (pinger.isActive()) {
                pinger.stop();
            }
        } catch (MqttException e) {
            log.info("MQTT Exception {}", e);
            delayedReconnect();
        }
    }

    public void delayedReconnect() {
        delayedReconnect(reconnectDelay);
    }

    public void delayedReconnect(long delayMs) {
        if (state == MqttConnectionState.RECONNECTING) {
            return;
        }
        // FIXME - reconnect flags are ugly. Seems to be a problem somewhere in the reconnect logic
        if (!shouldReconnect) {
            log.warn("Should not attempt to reconnect. Ignoring");
            return;
        }
        log.info("MQTT delayed reconnect");
        if (!timer.isActive()) {
            timer.registerObserver(this);
            timer.activate();
        }
        timer.setTimer(delayMs);
        state = MqttConnectionState.RECONNECTING;
        log.info(messages.getString("mqtt_reconnect"));
        reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                messages.getString("mqtt_reconnect"));
    }

    public void reconnect() {
        if (pinger.isNetworkActive()) {
            log.info("MQTT issuing reconnect");
            disconnect();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }).start();
        } else {
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_reconnect_fail"));
        }
    }

    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                log.info(messages.getString("mqtt_disconnected"));
                reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                        messages.getString("mqtt_disconnected"));
            }
            timer.deactivate();
            pinger.stop();
        } catch (MqttException e) {
            // TODO: determine how to handle this
            log.info(messages.getString("mqtt_disconnect_fail"));
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_disconnect_fail"));
        }
        state = MqttConnectionState.DISCONNECTED;
    }

    public void close() {
        try {
            disconnect();
            client.close();
            log.info(messages.getString("mqtt_close"));
            reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                    messages.getString("mqtt_close"));
        } catch (MqttException e) {
            // TODO: determine how to handle this. Cruton maybe?
            log.info(messages.getString("mqtt_close_fail"));
            reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                    messages.getString("mqtt_close_fail"));
        }
        pinger.unregisterObserver(this);
        timer.unregisterObserver(this);
    }


    @Override
    public void onFailedPing() {
        log.info(messages.getString("mqtt_ping_fail"));
        reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                messages.getString("mqtt_ping_fail"));
        notifyOnDisconnect();
        delayedReconnect();
    }

    @Override
    public void timerUp() {
        reconnect();
    }

    @Override
    public void connectionLost(Throwable cause) {
//        if (state != MqttConnectionState.CONNECTED) {
//            return;
//        }
        log.info(messages.getString("mqtt_lost_connection"));

        reporter.report(EventType.UPLOADER, EventSeverity.WARN,
                messages.getString("mqtt_lost_connection"));
        if (state != MqttConnectionState.RECONNECTING) {
            notifyOnDisconnect();
            delayedReconnect();
        }
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
//        List<String> mqTopics = Lists.newArrayList(topics);
//        List<String> mqTopics = new ArrayList<>();
//        mqTopics.addAll(topics)
        boolean willReconnect = false;
        for (String topic : topics) {
            try {
                client.subscribe(topic, QOS);
            } catch (MqttException e) {
                // TODO: Determine what to do here
                reporter.report(EventType.UPLOADER, EventSeverity.ERROR,
                        messages.getString("mqtt_subscribe_fail"));
                log.info(e.getMessage());
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
            reporter.report(EventType.UPLOADER, EventSeverity.INFO,
                    messages.getString("mqtt_publish_success"));
            log.info("{}: Published \"{}\" to \"{}\"", MqttEventMgr.class.getSimpleName(), Utils.bytesToHex(message), topic);
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
