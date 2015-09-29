package com.nightscout.core.mqtt;

public enum MqttConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    RECONNECTING
}
