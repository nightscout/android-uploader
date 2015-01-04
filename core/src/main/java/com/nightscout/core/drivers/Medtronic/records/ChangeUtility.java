package com.nightscout.core.drivers.Medtronic.records;

public class ChangeUtility extends DatedRecord {
    public static final byte bodySize = 0;

    ChangeUtility(byte[] data) {
        super(data);
    }
}
