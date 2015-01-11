package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Ian50 extends TimeStampedRecord {
    public Ian50(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 34;
        this.decode(data);
    }
}
