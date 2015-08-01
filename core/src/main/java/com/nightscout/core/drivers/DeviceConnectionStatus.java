package com.nightscout.core.drivers;

public class DeviceConnectionStatus {
    public DeviceType deviceType;
    public G4ConnectionState deviceState;

    public DeviceConnectionStatus(DeviceType deviceType, G4ConnectionState deviceState) {
        this.deviceType = deviceType;
        this.deviceState = deviceState;
    }
}
