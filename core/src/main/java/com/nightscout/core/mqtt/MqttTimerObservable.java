package com.nightscout.core.mqtt;


public interface MqttTimerObservable {
    public void registerObserver(MqttTimerObserver observer);

    public void unregisterObserver(MqttTimerObserver observer);
}
