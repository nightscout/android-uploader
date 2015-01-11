package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class PumpSuspended extends TimeStampedRecord {

    public PumpSuspended(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
