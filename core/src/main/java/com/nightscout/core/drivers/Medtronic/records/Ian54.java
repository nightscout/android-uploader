package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Ian54 extends TimeStampedRecord {
    public Ian54(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 57;
        this.decode(data);
    }
}
