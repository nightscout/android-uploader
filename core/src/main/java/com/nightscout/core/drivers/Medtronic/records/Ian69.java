package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Ian69 extends TimeStampedRecord {
    public Ian69(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 2;
        this.decode(data);
    }
}
