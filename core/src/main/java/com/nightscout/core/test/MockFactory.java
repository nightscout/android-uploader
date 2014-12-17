package com.nightscout.core.test;

import com.nightscout.core.dexcom.NoiseMode;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.records.DeviceStatus;

import org.joda.time.DateTime;

public class MockFactory {

  public static GlucoseDataSet mockGlucoseDataSet() {
    EGVRecord egvRecord = new EGVRecord(
        1,
        TrendArrow.DOUBLE_DOWN,
        new DateTime(0).toDate(),
        new DateTime(5).toDate(),
        NoiseMode.Clean);
    SensorRecord sensorRecord = new SensorRecord(new byte[SensorRecord.RECORD_SIZE]);
    return new GlucoseDataSet(egvRecord, sensorRecord);
  }

  public static MeterRecord mockMeterRecord() {
    return new MeterRecord(new byte[MeterRecord.RECORD_SIZE]);
  }

  public static CalRecord mockCalRecord() {
    return new CalRecord(new byte[CalRecord.RECORD_SIZE]);
  }

  public static DeviceStatus mockDeviceStatus() {
    DeviceStatus output = new DeviceStatus();
    output.setBatteryLevel(999);
    return output;
  }
}
