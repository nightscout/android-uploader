package com.nightscout.android.dexcom.records;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class GenericTimestampRecord {

    public final Date EPOCH = new GregorianCalendar(2009, 0, 1).getTime();
    private Date systemTime;
    private Date displayTime;

    public GenericTimestampRecord(byte[] packet) {
        int st = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        systemTime = getDate(st);
        int dt = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        displayTime = getDate(dt);
    }

    public Date getSystemTime() {
        return systemTime;
    }

    public Date getDisplayTime() {
        return displayTime;
    }

    // TODO: this will be used in 1 other place, thus might be best to get in a utilities class
    private Date getDate(int receiverTime) {
        // Epoch is PST, but but having epoch have user timezone added, then don't have to add to the
        // display time
        // TODO: probably best to do this Adriens way, TBD
        long milliseconds = EPOCH.getTime();
        long timeAdd = milliseconds + (1000L * receiverTime);
        TimeZone tz = TimeZone.getDefault();
        if (tz.inDaylightTime(new Date())) timeAdd = timeAdd - 3600000L;
        return new Date(timeAdd);
    }
}
