package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class LowReservoir extends TimeStampedRecord {

    public LowReservoir(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
