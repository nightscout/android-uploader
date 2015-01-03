package com.nightscout.core.dexcom;

import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.model.G4Noise;

import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EgvRecordTest {
//    EGV Record: C4881A0B61341A0B0500583E
//    EGV: 5 Trend: NOT_COMPUTABLE display time: 1417056321000, system time: 186288324, display time offset: 186266721, noise level: None
//
//    EGV Record: 80BD1A0B1D691A0B7800217D
//    EGV: 120 Trend: DOUBLE_UP display time: 1417069821000 system time: 186301824 noise level: None

    @Test
    public void shouldParseEgvRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0xC4, (byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58,
                (byte) 0x3E};
        EGVRecord egvRecord = new EGVRecord(record);
        assertThat(egvRecord.getBgMgdl(), is(5));
        assertThat(egvRecord.getTrend(), is(TrendArrow.NOT_COMPUTABLE));
        assertThat(egvRecord.getRawDisplayTimeSeconds(), is(186266721L));
        assertThat(egvRecord.getRawSystemTimeSeconds(), is(186288324L));
        assertThat(egvRecord.getNoiseMode(), is(G4Noise.NOT_COMPUTED));
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseSmallEgvRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0xC4, (byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58};
        EGVRecord egvRecord = new EGVRecord(record);
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseLargeEgvRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0xC4, (byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58,
                (byte) 0x3E, (byte) 0x00, (byte) 0x00};
        EGVRecord egvRecord = new EGVRecord(record);
    }

    @Test
    public void shouldConvertToJsonString() throws Exception {
        byte[] record = new byte[]{(byte) 0xC4, (byte) 0x88, (byte) 0x1A, (byte) 0x0B, (byte) 0x61,
                (byte) 0x34, (byte) 0x1A, (byte) 0x0B, (byte) 0x05, (byte) 0x00, (byte) 0x58,
                (byte) 0x3E};
        JSONObject obj = new JSONObject();
        obj.put("sgv", 5);
        obj.put("date", Utils.receiverTimeToDate(186266721));
        EGVRecord egvRecord = new EGVRecord(record);
        assertThat(egvRecord.toJSON().toString(), is(obj.toString()));
    }
}
