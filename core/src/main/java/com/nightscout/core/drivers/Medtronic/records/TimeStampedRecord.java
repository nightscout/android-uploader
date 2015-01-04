package com.nightscout.core.drivers.Medtronic.records;

import org.joda.time.DateTime;


abstract public class TimeStampedRecord extends Record {
    public static final byte dateSize = 5;
    protected DateTime timeStamp;

    TimeStampedRecord(byte[] data) {
        super(data);
        totalSize += dateSize;
        parseDate(data);
    }

    private DateTime parseDate(byte[] data){
        int seconds = data[2] & 0x3F;
        int month = (data[2] & 0xC0) << 2 | (data[3] & 0xC0);
        int minutes = data[3] & 0x3F;
        int hour = data[4] & 0x1F;
        int dayOfMonth = data[5] & 0x1F;
        int year = data[6] & 0x3F; // Assuming this is correct, need to verify. Otherwise this will be a problem in 2016.
        return new DateTime(year, month, dayOfMonth, hour, minutes, seconds);
    }

    public DateTime getTimeStamp() {
        return timeStamp;
    }
}
