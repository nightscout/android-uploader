package com.nightscout.core.records;

public class DeviceStatus {

  private int batteryLevel;

  public DeviceStatus() {
    batteryLevel = -1;
  }

  public static DeviceStatus getCurrentStatus() {
    return null;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public void setBatteryLevel(int batteryLevel) {
    this.batteryLevel = batteryLevel;
  }
}
