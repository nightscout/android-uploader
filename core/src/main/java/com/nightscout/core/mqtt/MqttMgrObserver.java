package com.nightscout.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttMgrObserver {
    public void onMessage(String topic, MqttMessage message);

    public void onDisconnect();
}