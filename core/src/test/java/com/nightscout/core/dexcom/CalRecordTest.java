package com.nightscout.core.dexcom;

import com.nightscout.core.dexcom.records.CalRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CalRecordTest {
    private byte[] record505;
    private byte[] recordPre505;

    @Before
    public void setUp() {
        record505 = new byte[]{(byte) 0x1E, (byte) 0xF9, (byte) 0x1A, (byte) 0x0B, (byte) 0xD9,
                (byte) 0x7B, (byte) 0x1A, (byte) 0x0B, (byte) 0x92, (byte) 0xEE, (byte) 0x4C,
                (byte) 0x6B, (byte) 0x71, (byte) 0x77, (byte) 0x88, (byte) 0x40, (byte) 0x58,
                (byte) 0xE9, (byte) 0xC6, (byte) 0xB1, (byte) 0x02, (byte) 0x0C, (byte) 0xDC,
                (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xF0, (byte) 0x3F, (byte) 0x03, (byte) 0x06, (byte) 0x01,
                (byte) 0xAB, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA,
                (byte) 0x00, (byte) 0x40, (byte) 0x03, (byte) 0x0C, (byte) 0xDE, (byte) 0x1A,
                (byte) 0x0B, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,
                (byte) 0x49, (byte) 0x01, (byte) 0x00, (byte) 0x56, (byte) 0xDF, (byte) 0x1A,
                (byte) 0x0B, (byte) 0x00, (byte) 0x23, (byte) 0xDE, (byte) 0x1A, (byte) 0x0B,
                (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x49,
                (byte) 0x01, (byte) 0x00, (byte) 0x56, (byte) 0xDF, (byte) 0x1A, (byte) 0x0B,
                (byte) 0x00, (byte) 0xE0, (byte) 0xF7, (byte) 0x1A, (byte) 0x0B, (byte) 0x06,
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x91, (byte) 0x03,
                (byte) 0x00, (byte) 0x1D, (byte) 0xF9, (byte) 0x1A, (byte) 0x0B, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC7};
        recordPre505 = new byte[]{(byte) 0x6B, (byte) 0xD1, (byte) 0x1A, (byte) 0x0B, (byte) 0x09,
                (byte) 0x7D, (byte) 0x1A, (byte) 0x0B, (byte) 0xD1, (byte) 0x3C, (byte) 0xE9,
                (byte) 0x4A, (byte) 0xB3, (byte) 0x6C, (byte) 0x89, (byte) 0x40, (byte) 0x64,
                (byte) 0x75, (byte) 0x6A, (byte) 0xCF, (byte) 0xE2, (byte) 0xEB, (byte) 0xD9,
                (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xF0, (byte) 0x3F, (byte) 0x03, (byte) 0x06, (byte) 0x01,
                (byte) 0x1E, (byte) 0x10, (byte) 0x09, (byte) 0xD2, (byte) 0xA6, (byte) 0x30,
                (byte) 0xF2, (byte) 0x3F, (byte) 0x03, (byte) 0x2E, (byte) 0xB5, (byte) 0x1A,
                (byte) 0x0B, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50,
                (byte) 0x49, (byte) 0x01, (byte) 0x00, (byte) 0x77, (byte) 0xB6, (byte) 0x1A,
                (byte) 0x0B, (byte) 0x00, (byte) 0x45, (byte) 0xB5, (byte) 0x1A, (byte) 0x0B,
                (byte) 0x48, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x49,
                (byte) 0x01, (byte) 0x00, (byte) 0x77, (byte) 0xB6, (byte) 0x1A, (byte) 0x0B,
                (byte) 0x00, (byte) 0x5E, (byte) 0xD0, (byte) 0x1A, (byte) 0x0B, (byte) 0x06,
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x60, (byte) 0xA8, (byte) 0x03,
                (byte) 0x00, (byte) 0x6B, (byte) 0xD1, (byte) 0x1A, (byte) 0x0B, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x33, (byte) 0x33,
                (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0xD3, (byte) 0x3F,
                (byte) 0x55, (byte) 0x55, (byte) 0x55, (byte) 0x55, (byte) 0x55, (byte) 0x55,
                (byte) 0xE5, (byte) 0x3F, (byte) 0x03};
    }

    @Test
    public void shouldParsePre505CalRecord() throws Exception {
        CalRecord calRecord = new CalRecord(recordPre505);
        assertThat(calRecord.getSlope(), is(813.5875452253041));
        assertThat(calRecord.getIntercept(), is(26543.54390965904));
        assertThat(calRecord.getScale(), is(1.0));
        assertThat(calRecord.getDecay(), is(1.1368778423793695));
        assertThat(calRecord.getNumRecords(), is(3));
        assertThat(calRecord.getRawDisplayTimeSeconds(), is(186285321L));
        assertThat(calRecord.getRawSystemTimeSeconds(), is(186306923L));
    }

    @Test
    public void shouldParse505CalRecord() throws Exception {
        CalRecord calRecord = new CalRecord(record505);
        assertThat(calRecord.getSlope(), is(782.9303804407411));
        assertThat(calRecord.getIntercept(), is(28720.042100646853));
        assertThat(calRecord.getScale(), is(1.0));
        assertThat(calRecord.getNumRecords(), is(3));
        assertThat(calRecord.getDecay(), is(2.0833333333333335));
        assertThat(calRecord.getRawDisplayTimeSeconds(), is(186285017L));
        assertThat(calRecord.getRawSystemTimeSeconds(), is(186317086L));
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseSmallCal505Record() throws Exception {
        record505 = Arrays.copyOfRange(record505, 0, record505.length - 1);
        CalRecord calRecord = new CalRecord(record505);
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseSmallCalPre505Record() throws Exception {
        recordPre505 = Arrays.copyOfRange(recordPre505, 0, recordPre505.length - 1);
        CalRecord calRecord = new CalRecord(recordPre505);
    }


    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseLargeCal505Record() throws Exception {
        record505 = Arrays.copyOf(record505, record505.length + 1);
        CalRecord calRecord = new CalRecord(record505);
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseLargeCalPre505Record() throws Exception {
        recordPre505 = Arrays.copyOf(recordPre505, recordPre505.length + 1);
        CalRecord calRecord = new CalRecord(recordPre505);
    }


    @After
    public void tearDown() {
        recordPre505 = null;
        record505 = null;
    }

}
