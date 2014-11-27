package com.nightscout.core.dexcom;

import com.nightscout.core.dexcom.records.EGVRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnit4.class)
public class EgvRecordTest {
//    EGV Record: C4881A0B61341A0B0500583E
//    EGV: 5 Trend: NOT_COMPUTABLE display time: 1417056321000 system time: 186288324 noise level: None
//
//    EGV Record: 80BD1A0B1D691A0B7800217D
//    EGV: 120 Trend: DOUBLE_UP display time: 1417069821000 system time: 186301824 noise level: None

    @Test
    public void isSpecialValue() {
        byte[] record = new byte[]{ (byte) 0xC4,(byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58,
                (byte) 0x3E };
        EGVRecord egvRecord=new EGVRecord(record);
        assertThat(egvRecord.isSpecialValue(),is(true));
    }

    @Test
    public void shouldParseRecord() {
        byte[] record = new byte[]{ (byte) 0xC4,(byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58,
                (byte) 0x3E };
        EGVRecord egvRecord=new EGVRecord(record);
        assertThat(egvRecord.getBGValue(), is(5));
        assertThat(egvRecord.getTrend(), is(TrendArrow.NOT_COMPUTABLE));
        assertThat(egvRecord.getDisplayTimeSeconds(), is(1417056321000L));
        assertThat(egvRecord.getSystemTimeSeconds(), is(186288324));
        assertThat(egvRecord.getNoiseMode(), is (NoiseMode.None));
    }
}
