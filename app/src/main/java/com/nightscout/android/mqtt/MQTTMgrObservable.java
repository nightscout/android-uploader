package com.nightscout.android.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MQTTMgrObservable {
    public void registerObserver(MQTTMgrObserverInterface observer);
    public void unregisterObserver(MQTTMgrObserverInterface observer);
    public void notifyObservers(String topic, MqttMessage message);
    public void notifyDisconnect();
}
