package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class IanA8 extends TimeStampedRecord {
    public IanA8(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 11;
        this.decode(data);
    }
}
