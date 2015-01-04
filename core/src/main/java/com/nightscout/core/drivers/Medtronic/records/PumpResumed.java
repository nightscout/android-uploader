package com.nightscout.core.drivers.Medtronic.records;

public class PumpResumed extends DatedRecord {
    public static final byte bodySize = 0;

    PumpResumed(byte[] data) {
        super(data);
    }
}
