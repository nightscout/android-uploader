package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class TempBasalRate extends TimeStampedRecord {

    public TempBasalRate(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 1;
        this.decode(data);
    }
}