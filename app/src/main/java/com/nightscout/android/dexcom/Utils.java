package com.nightscout.android.dexcom;

import java.util.Date;
import java.util.TimeZone;

public class Utils {

    public static Date receiverTimeToDate(int delta) {
        int currentTZOffset = TimeZone.getDefault().getRawOffset();
        long epochMS = 1230768000000L;  // Jan 01, 2009 00:00 in UTC
        long milliseconds = epochMS - currentTZOffset;
        long timeAdd = milliseconds + (1000L * delta);
        TimeZone tz = TimeZone.getDefault();
        if (tz.inDaylightTime(new Date())) timeAdd = timeAdd - 3600000L;
        return new Date(timeAdd);
    }
}
