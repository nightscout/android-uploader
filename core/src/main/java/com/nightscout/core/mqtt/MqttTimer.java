package com.nightscout.core.mqtt;

public interface MqttTimer {
    public void setTimer(long delayMs);

    public void cancel();

    public void activate();

    public void deactivate();

    public boolean isActive();

    public void registerObserver(MqttTimerObserver observer);

    public void unregisterObserver(MqttTimerObserver observer);
}
