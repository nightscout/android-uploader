package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class TempBasalDuration extends TimeStampedRecord {

    public TempBasalDuration(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}