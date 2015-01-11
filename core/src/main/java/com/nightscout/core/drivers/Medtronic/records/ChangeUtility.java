package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ChangeUtility extends TimeStampedRecord {

    public ChangeUtility(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
