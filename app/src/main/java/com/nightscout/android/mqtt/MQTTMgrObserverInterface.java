package com.nightscout.android.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MQTTMgrObserverInterface {
    public void onMessage(String topic, MqttMessage message);
    public void onDisconnect();
}
