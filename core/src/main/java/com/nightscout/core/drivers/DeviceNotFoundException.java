package com.nightscout.core.drivers;

public class DeviceNotFoundException extends RuntimeException {
  public DeviceNotFoundException(DeviceType type) {
    super("Could not find device of type " + type.name());
  }
}
