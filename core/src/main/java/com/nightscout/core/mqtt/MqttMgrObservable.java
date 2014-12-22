package com.nightscout.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttMgrObservable {
    public void registerObserver(MqttMgrObserver observer);

    public void unregisterObserver(MqttMgrObserver observer);

    public void notifyOnMessage(String topic, MqttMessage message);

    public void notifyOnDisconnect();
}