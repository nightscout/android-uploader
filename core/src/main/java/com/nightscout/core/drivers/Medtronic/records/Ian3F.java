package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Ian3F extends TimeStampedRecord {
  public Ian3F(byte[] data, PumpModel model) {
    super(data, model);
    bodySize = 3;
    this.decode(data);
  }
}