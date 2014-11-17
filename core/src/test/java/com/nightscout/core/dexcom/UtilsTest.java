package com.nightscout.core.dexcom;

import com.google.common.primitives.UnsignedBytes;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class UtilsTest {

    // TODO(trhodeos): remove this test.
    @Test
    public void testReceiverTimeToDate() {
        long epochMS = 1230768000000L;  // Jan 01, 2009 00:00 in UTC
        // Purposefully lose some precision here
        long currentTime = (new Date().getTime() / 1000L) * 1000L;
        int currentTZOffset = TimeZone.getDefault().getRawOffset();
        long milliseconds = epochMS - currentTZOffset;
        long delta = (currentTime - milliseconds);
        TimeZone tz = TimeZone.getDefault();
        if (tz.inDaylightTime(new Date())) {
            delta = delta + 1 * 60 * 60 * 1000;
        }
        Date currentDateObj = Utils.receiverTimeToDate(delta / 1000L);
        assertThat(currentDateObj.getTime(), is(currentTime));
    }

    @Test
    public void testReceiverTimeToDateTime_epoch() {
        assertThat(Utils.receiverTimeToDateTime(0),
                is(Utils.DEXCOM_EPOCH.withZone(DateTimeZone.getDefault())));
    }

    @Test
    public void testReceiverTimeToDateTime_positiveDelta() {
        int secondsDelta = 10;
        assertThat(Utils.receiverTimeToDateTime(secondsDelta),
                is(Utils.DEXCOM_EPOCH.plusSeconds(secondsDelta).withZone(DateTimeZone.getDefault())));
    }

    @Test
    public void testReceiverTimeToDateTime_negativeDelta() {
        int secondsDelta = -10;
        assertThat(Utils.receiverTimeToDateTime(secondsDelta),
                is(Utils.DEXCOM_EPOCH.minusSeconds(10).withZone(DateTimeZone.getDefault())));
    }

    @Test
    public void testGetTimeAgoString_ZeroDelta() {
        DateTime now = new DateTime();
        assertThat(Utils.getTimeAgoString(new Period(now, now)), is("0 seconds ago"));
    }

    @Test
    public void testGetTimeAgoString_SecDelta() {
        DateTime now = new DateTime();
        assertThat(Utils.getTimeAgoString(new Period(now, now.plusSeconds(1))),
                is("1 seconds ago"));
    }

    @Test
    public void testGetTimeAgoString_DayDelta() {
        DateTime now = new DateTime();
        assertThat(Utils.getTimeAgoString(new Period(now, now.plusDays(1))),
                is("1 days ago"));
    }

    @Test
    public void testGetTimeAgoString_Multiple() {
        DateTime now = new DateTime();
        assertThat(Utils.getTimeAgoString(new Period(now,
                        now.plusSeconds(3).plusDays(1).plusMonths(1))),
                is("3 seconds, 1 days, and 1 months ago"));
    }

    @Test
    public void testBytesToHex_Simple() {
        assertThat(Utils.bytesToHex(new byte[]{0xA}), is("0A"));
    }

    @Test
    public void testBytesToHex_Multiple() {
        assertThat(Utils.bytesToHex(new byte[]{
                        UnsignedBytes.checkedCast(0xDE),
                        UnsignedBytes.checkedCast(0xAD),
                        UnsignedBytes.checkedCast(0xBE),
                        UnsignedBytes.checkedCast(0xEF)}),
                is("DEADBEEF"));
    }

}
