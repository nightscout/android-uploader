package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Ian0B extends TimeStampedRecord {
    public Ian0B(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 4;
        this.decode(data);
    }
}
