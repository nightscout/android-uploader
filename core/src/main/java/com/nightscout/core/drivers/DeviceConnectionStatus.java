package com.nightscout.core.drivers;

public class DeviceConnectionStatus {
    public SupportedDevices deviceType;
    public G4ConnectionState deviceState;

    public DeviceConnectionStatus(SupportedDevices deviceType, G4ConnectionState deviceState) {
        this.deviceType = deviceType;
        this.deviceState = deviceState;
    }
}
