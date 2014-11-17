package com.nightscout.core.test;

import com.nightscout.core.records.DeviceStatus;
import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;

import org.joda.time.DateTime;

public class MockFactory {
    public static GlucoseDataSet mockGlucoseDataSet() {
        EGVRecord egvRecord = new EGVRecord(
                1,
                Constants.TREND_ARROW_VALUES.DOUBLE_DOWN,
                new DateTime(0).toDate(),
                new DateTime(5).toDate());
        SensorRecord sensorRecord = new SensorRecord(new byte[128]);
        return new GlucoseDataSet(egvRecord, sensorRecord);
    }

    public static MeterRecord mockMeterRecord() {
        return new MeterRecord(new byte[128]);
    }

    public static CalRecord mockCalRecord() {
        return new CalRecord(new byte[128]);
    }

    public static DeviceStatus mockDeviceStatus() {
        DeviceStatus output = new DeviceStatus();
        output.setBatteryLevel(999);
        return output;
    }
}
