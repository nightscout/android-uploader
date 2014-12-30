// Meter Record: 2880180BC52B180B71000A80180BAC
// Record EGV: 113 Meter time: 186155018 display time: 1416926645000 system time: 186155048
// Meter Record: 4CB51A0BE9601A0B46002EB51A0B73
// Record EGV: 70 Meter time: 186299694 display time: 1417071321000 system time: 186299724
// Meter Record: 63B51A0B00611A0B480045B51A0BB1
// Record EGV: 72 Meter time: 186299717 display time: 1417071344000 system time: 186299747
// Meter Record: 7CD01A0B1A7C1A0B06015ED01A0B06
// Record EGV: 262 Meter time: 186306654 display time: 1417078282000 system time: 186306684
// Meter Record: 2880180BC52B180B71000A80180BAC
// Record EGV: 113 Meter time: 186155018 display time: 1416926645000 system time: 186155048
// Meter Record: 4CB51A0BE9601A0B46002EB51A0B73
// Record EGV: 70 Meter time: 186299694 display time: 1417071321000 system time: 186299724
// Meter Record: 63B51A0B00611A0B480045B51A0BB1
// Record EGV: 72 Meter time: 186299717 display time: 1417071344000 system time: 186299747
// Meter Record: 7CD01A0B1A7C1A0B06015ED01A0B06
// Record EGV: 262 Meter time: 186306654 display time: 1417078282000 system time: 186306684

package com.nightscout.core.dexcom;

import com.nightscout.core.dexcom.records.MeterRecord;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MeterRecordTest {
    // Meter Record: 28 80 18 0B C5 2B 18 0B 71 00 0A 80 18 0B AC
    // Record EGV: 113 Meter time: 186155018 display time: 1416926645000 system time: 186155048
    // Meter Record: 7CD01A0B1A7C1A0B06015ED01A0B06
    // Record EGV: 262 Meter time: 186306654 display time: 1417078282000 system time: 186306684

    @Test
    public void shouldParseMeterRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x28, (byte) 0x80, (byte) 0x18, (byte) 0x0B, (byte) 0xC5,
                (byte) 0x2B, (byte) 0x18, (byte) 0x0B, (byte) 0x71, (byte) 0x00, (byte) 0x0A,
                (byte) 0x80, (byte) 0x18, (byte) 0x0B, (byte) 0xAC};
        MeterRecord meterRecord = new MeterRecord(record);
        assertThat(meterRecord.getBgMgdl(), is(113));
        assertThat(meterRecord.getRawDisplayTimeSeconds(), is(186133445L));
        assertThat(meterRecord.getRawSystemTimeSeconds(), is(186155048L));
        assertThat(meterRecord.getMeterTime(), is(186155018));
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseSmallMeterRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x28, (byte) 0x80, (byte) 0x18, (byte) 0x0B, (byte) 0xC5,
                (byte) 0x2B, (byte) 0x18, (byte) 0x0B, (byte) 0x71, (byte) 0x00, (byte) 0x0A,
                (byte) 0x80, (byte) 0x18, (byte) 0x0B};
        MeterRecord meterRecord = new MeterRecord(record);
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseLargeMeterRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x28, (byte) 0x80, (byte) 0x18, (byte) 0x0B, (byte) 0xC5,
                (byte) 0x2B, (byte) 0x18, (byte) 0x0B, (byte) 0x71, (byte) 0x00, (byte) 0x0A,
                (byte) 0x80, (byte) 0x18, (byte) 0x0B, (byte) 0x00, (byte) 0x00};
        MeterRecord meterRecord = new MeterRecord(record);
    }
}
