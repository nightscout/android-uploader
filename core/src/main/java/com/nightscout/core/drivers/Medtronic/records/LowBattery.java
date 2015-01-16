package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class LowBattery extends TimeStampedRecord {
    public LowBattery(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}