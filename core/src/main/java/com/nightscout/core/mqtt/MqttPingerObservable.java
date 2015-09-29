package com.nightscout.core.mqtt;

public interface MqttPingerObservable {
    public void registerObserver(MqttPingerObserver observer);

    public void unregisterObserver(MqttPingerObserver observer);
}
