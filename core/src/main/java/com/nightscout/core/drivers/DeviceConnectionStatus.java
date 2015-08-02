package com.nightscout.core.drivers;

public class DeviceConnectionStatus {
    public DeviceType deviceType;
    public DeviceConnectionState deviceState;

    public DeviceConnectionStatus(DeviceType deviceType, DeviceConnectionState deviceState) {
        this.deviceType = deviceType;
        this.deviceState = deviceState;
    }
}
