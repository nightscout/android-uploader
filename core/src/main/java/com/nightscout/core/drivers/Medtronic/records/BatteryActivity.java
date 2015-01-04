package com.nightscout.core.drivers.Medtronic.records;

public class BatteryActivity extends DatedRecord {
    public static final byte bodySize = 0;

    BatteryActivity(byte[] data) {
        super(data);
    }
}
