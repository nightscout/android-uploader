package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ToggleRemote extends TimeStampedRecord {

    public ToggleRemote(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 14;
        this.decode(data);
    }
}
