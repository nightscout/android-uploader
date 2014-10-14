package com.nightscout.android.test;

import junit.framework.TestCase;

import com.nightscout.android.TimeConstants;
import com.nightscout.android.dexcom.Utils;

import java.util.Date;
import java.util.TimeZone;

public class UtilsTest extends TestCase {
    public void testTimeString(){
        String timeStr=Utils.getTimeString(60*1000);
        assertEquals("1 min ago",timeStr);
        timeStr=Utils.getTimeString(60*1000*60);
        assertEquals("1 hours ago",timeStr);
        timeStr=Utils.getTimeString(60*1000*60*24);
        assertEquals("1 days ago",timeStr);
        timeStr=Utils.getTimeString(60*1000*60*24*7);
        assertEquals("1 weeks ago",timeStr);
    }

    public void testReceiverTimeToDate(){
        long epochMS = 1230768000000L;  // Jan 01, 2009 00:00 in UTC
        // Purposefully lose some precision here
        long currentTime=(new Date().getTime()/1000L)*1000L;
        int currentTZOffset = TimeZone.getDefault().getRawOffset();
        long milliseconds = epochMS - currentTZOffset;
        long delta=(currentTime-milliseconds);
        TimeZone tz = TimeZone.getDefault();
        if (tz.inDaylightTime(new Date())) delta = delta + TimeConstants.ONE_HOUR_MS;
        Date currentDateObj=Utils.receiverTimeToDate(delta/1000L);
        assertEquals(currentTime,currentDateObj.getTime());
    }
}
