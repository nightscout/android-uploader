package com.nightscout.core.drivers.Medtronic.records;

public class PumpSuspended extends DatedRecord {
    public static final byte bodySize = 0;

    PumpSuspended(byte[] data) {
        super(data);
    }
}
