package com.nightscout.core.dexcom;

import com.google.common.primitives.UnsignedBytes;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UtilsTest {

    @Test
    public void testReceiverTimeToDateTime_epoch() {
        assertThat(Utils.receiverTimeToDateTime(0),
                is(Utils.DEXCOM_EPOCH.withZone(DateTimeZone.UTC)));
    }

    @Test
    public void testReceiverTimeToDateTime_positiveDelta() {
        int secondsDelta = 10;
        assertThat(Utils.receiverTimeToDateTime(secondsDelta),
                is(Utils.DEXCOM_EPOCH.plusSeconds(secondsDelta).withZone(DateTimeZone.UTC)));
    }

    @Test
    public void testReceiverTimeToDateTime_negativeDelta() {
        int secondsDelta = -10;
        assertThat(Utils.receiverTimeToDateTime(secondsDelta),
                is(Utils.DEXCOM_EPOCH.minusSeconds(10).withZone(DateTimeZone.UTC)));
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
                        now.plusMonths(1).plusDays(1).plusSeconds(3))),
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
