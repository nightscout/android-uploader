package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class BatteryActivity extends TimeStampedRecord {

    public BatteryActivity(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}