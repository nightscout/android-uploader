package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class PumpResumed extends TimeStampedRecord {

    public PumpResumed(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
