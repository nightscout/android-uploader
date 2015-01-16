package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

import org.joda.time.DateTime;


abstract public class TimeStampedRecord extends Record {
    protected DateTime timeStamp;

    public TimeStampedRecord(byte[] data, PumpModel model) {
        super(data, model);
        timestampSize = 5;
        timeStamp = parseDate(data);
    }

    @Override
    protected void decode(byte[] data) {
        timeStamp = parseDate(data);
    }

    private DateTime parseDate(byte[] data){
        int seconds = data[headerSize] & 0x3F;
        int month = ((data[headerSize] & 0xC0) >> 6) | ((data[headerSize + 1] & 0xC0) >> 4);
        int minutes = data[headerSize + 1] & 0x3F;
        int hour = data[headerSize + 2] & 0x1F;
        int dayOfMonth = data[headerSize + 3] & 0x1F;
        int year = data[headerSize + 4] & 0x3F; // Assuming this is correct, need to verify. Otherwise this will be a problem in 2016.
        return new DateTime(year + 2000, month, dayOfMonth, hour, minutes, seconds);
    }

    public DateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public void logRecord() {
        log.info("Time stamped record ({}): {}", recordTypeName, timeStamp.toString());
    }
}