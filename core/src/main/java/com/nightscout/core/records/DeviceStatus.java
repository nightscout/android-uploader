package com.nightscout.core.records;

public class DeviceStatus {
    private int batteryLevel;

    public DeviceStatus() {
        batteryLevel = -1;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public static DeviceStatus getCurrentStatus() {
        return null;
    }
}
