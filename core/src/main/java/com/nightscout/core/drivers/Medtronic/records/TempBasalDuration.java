package com.nightscout.core.drivers.Medtronic.records;

public class TempBasalDuration extends DatedRecord {
    public static final byte bodySize = 0;

    TempBasalDuration(byte[] data) {
        super(data);
    }
}
