package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class SelectBasalProfile extends TimeStampedRecord {
    public SelectBasalProfile(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}
