// Sensor Record: 56301B0BF3DB1A0BC03B020050FD0100A600C7
// Record Sensor filtered: 130384 unfiltered: 146368 RSSI: 166 display time: 1417102819000 system time: 186331222
// Sensor Record: 82311B0B1FDD1A0B2058020080220200A30092
// Record Sensor filtered: 139904 unfiltered: 153632 RSSI: 163 display time: 1417103119000 system time: 186331522


package com.nightscout.core.dexcom;

import com.nightscout.core.dexcom.records.SensorRecord;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class SensorRecordTest {

    @Test
    public void shouldParseSensorRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x56, (byte) 0x30, (byte) 0x1B, (byte) 0x0B, (byte) 0xF3,
                (byte) 0xDB, (byte) 0x1A, (byte) 0x0B, (byte) 0xC0, (byte) 0x3B, (byte) 0x02,
                (byte) 0x00, (byte) 0x50, (byte) 0xFD, (byte) 0x01, (byte) 0x00, (byte) 0xA6,
                (byte) 0x00, (byte) 0xC7};
        SensorRecord sensorRecord = new SensorRecord(record);
        assertThat(sensorRecord.getUnfiltered(), is(146368L));
        assertThat(sensorRecord.getFiltered(), is(130384L));
        assertThat(sensorRecord.getRssi(), is(166));
        assertThat(sensorRecord.getRawDisplayTimeSeconds(), is(186309619L));
        assertThat(sensorRecord.getRawSystemTimeSeconds(), is(186331222L));
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseSmallSensorRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x56, (byte) 0x30, (byte) 0x1B, (byte) 0x0B, (byte) 0xF3,
                (byte) 0xDB, (byte) 0x1A, (byte) 0x0B, (byte) 0xC0, (byte) 0x3B, (byte) 0x02,
                (byte) 0x00, (byte) 0x50, (byte) 0xFD, (byte) 0x01, (byte) 0x00, (byte) 0xA6,
                (byte) 0x00};
        SensorRecord sensorRecord = new SensorRecord(record);
    }

    @Test(expected = InvalidRecordLengthException.class)
    public void shouldNotParseLargeSensorRecord() throws Exception {
        byte[] record = new byte[]{(byte) 0x56, (byte) 0x30, (byte) 0x1B, (byte) 0x0B, (byte) 0xF3,
                (byte) 0xDB, (byte) 0x1A, (byte) 0x0B, (byte) 0xC0, (byte) 0x3B, (byte) 0x02,
                (byte) 0x00, (byte) 0x50, (byte) 0xFD, (byte) 0x01, (byte) 0x00, (byte) 0xA6,
                (byte) 0x00, (byte) 0xC7, (byte) 0x00};
        SensorRecord sensorRecord = new SensorRecord(record);
    }

}