package com.nightscout.core.drivers.Medtronic.records;


public class ChangeTimeDisplay extends DatedRecord {
    public static final byte bodySize = 0;

    ChangeTimeDisplay(byte[] data) {
        super(data);
    }
}
