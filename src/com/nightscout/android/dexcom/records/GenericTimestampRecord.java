package com.nightscout.android.dexcom.records;

import java.util.Date;

public class GenericTimestampRecord {

    public final String EPOCH = "01-01-2009";
    private Date systemTime;
    private Date displayTime;

    public GenericTimestampRecord(byte[] packet) {

    }

    public Date getSystemTime() {
        return systemTime;
    }

    public Date getDisplayTime() {
        return displayTime;
    }
}
