package com.nightscout.core.drivers.Medtronic.records;

public class ToggleRemote extends DatedRecord {
    public static final byte bodySize = 14;

    ToggleRemote(byte[] data) {
        super(data);
    }
}
