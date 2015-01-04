package com.nightscout.core.drivers.Medtronic.records;

public class LowReservoir extends DatedRecord {
    public static final byte bodySize = 0;

    LowReservoir(byte[] data) {
        super(data);
    }
}
