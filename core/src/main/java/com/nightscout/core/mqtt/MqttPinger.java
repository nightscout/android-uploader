package com.nightscout.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;

public interface MqttPinger extends MqttPingerObservable {
    public void ping();

    public void start();

    public void stop();

    public boolean isActive();

    public void reset();

    public boolean isNetworkActive();

    public void setKeepAliveInterval(int ms);

    public void setMqttClient(MqttClient mqttClient);
}
