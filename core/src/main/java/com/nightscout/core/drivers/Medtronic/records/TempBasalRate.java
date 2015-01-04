package com.nightscout.core.drivers.Medtronic.records;

public class TempBasalRate extends DatedRecord {
    public static final byte bodySize = 1;

    TempBasalRate(byte[] data) {
        super(data);
    }
}
