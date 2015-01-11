package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class NewTimeSet extends TimeStampedRecord {
    public NewTimeSet(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
