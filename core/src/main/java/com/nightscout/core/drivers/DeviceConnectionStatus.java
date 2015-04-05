package com.nightscout.core.drivers;

public class DeviceConnectionStatus {
    public SupportedDevices deviceType;
    public DeviceState deviceState;

    public DeviceConnectionStatus(SupportedDevices deviceType, DeviceState deviceState) {
        this.deviceType = deviceType;
        this.deviceState = deviceState;
    }
}
