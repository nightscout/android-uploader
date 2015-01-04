package com.nightscout.core.drivers.Medtronic.records;


public class ChangeRemoteId extends DatedRecord {
    public static final byte bodySize = 0;

    ChangeRemoteId(byte[] data) {
        super(data);
    }
}
