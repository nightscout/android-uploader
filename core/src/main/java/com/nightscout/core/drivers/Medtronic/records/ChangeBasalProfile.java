package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ChangeBasalProfile extends TimeStampedRecord {
    public ChangeBasalProfile(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 145;
        this.decode(data);
    }
}