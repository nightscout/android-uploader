package com.nightscout.core.mqtt;

public interface MqttPingerObserver {
    public void onFailedPing();
}