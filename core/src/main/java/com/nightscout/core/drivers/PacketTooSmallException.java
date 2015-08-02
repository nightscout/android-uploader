package com.nightscout.core.drivers;

import java.io.IOException;

public class PacketTooSmallException extends IOException {
  public PacketTooSmallException(final String s) {
    super(s);
  }
}
