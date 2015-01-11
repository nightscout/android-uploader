package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ChangeTime extends TimeStampedRecord {
    public ChangeTime(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
