package com.nightscout.android.db;

import com.google.common.collect.Lists;

import com.nightscout.android.db.model.DeviceRecord;
import com.nightscout.android.db.model.ProtoRecord;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.Insertion;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.orm.query.Condition;
import com.orm.query.Select;

import net.tribe7.common.base.Optional;

import java.util.List;

public final class DbUtils {

  private DbUtils() {}

  public static Optional<ProtoRecord> getNewestElementInDb(DeviceType deviceType) {
    switch (deviceType) {
      case DEXCOM_G4:
      case DEXCOM_G4_SHARE2:
        return getNewestG4SensorGlucoseValueInDb();
      default:
        return Optional.absent();
    }
  }

  /**
   * Gets the Timestamped object with the oldest 'most recently updated' timestamp.
   * This is to ensure we grab all possible newly updated data from the dexcom.
   */
  private static Optional<ProtoRecord> getNewestG4SensorGlucoseValueInDb() {
    ProtoRecord protoRecord = Select.from(ProtoRecord.class).where(
        Condition.prop("record_type").like("G4_SENSOR_GLUCOSE_VALUE"))
        .orderBy("timestamp_sec DESC").first();
    return Optional.fromNullable(protoRecord);
  }

  public static void updateAllRecords(Download download) {
    if (download.g4_data != null) {
      updateAllG4Records(download.g4_data);
    }
  }

  private static int updateAllG4Records(G4Data g4Data) {
    List<ProtoRecord> recordList = Lists.newArrayList();
    if (g4Data.sensor_glucose_values != null) {
      for (SensorGlucoseValue sensorGlucoseValue : g4Data.sensor_glucose_values) {
        recordList.add(new ProtoRecord(sensorGlucoseValue.timestamp.system_time_sec,
                                       ProtoRecord.RecordType.G4_SENSOR_GLUCOSE_VALUE, sensorGlucoseValue.toByteArray()));
      }
    }
    if (g4Data.manual_meter_entries != null) {
      for (ManualMeterEntry manualMeterEntry: g4Data.manual_meter_entries) {
        recordList.add(new ProtoRecord(manualMeterEntry.timestamp.system_time_sec,
                                       ProtoRecord.RecordType.G4_MANUAL_METER_ENTRY, manualMeterEntry.toByteArray()));
      }
    }
    if (g4Data.raw_sensor_readings != null) {
      for (RawSensorReading rawSensorReading: g4Data.raw_sensor_readings) {
        recordList.add(new ProtoRecord(rawSensorReading.timestamp.system_time_sec,
                                       ProtoRecord.RecordType.G4_RAW_SENSOR_READING, rawSensorReading.toByteArray()));
      }
    }
    if (g4Data.calibrations != null) {
      for (Calibration calibration: g4Data.calibrations) {
        recordList.add(new ProtoRecord(calibration.timestamp.system_time_sec,
                                       ProtoRecord.RecordType.G4_CALIBRATION, calibration.toByteArray()));
      }
    }
    if (g4Data.insertions != null) {
      for (Insertion insertion: g4Data.insertions) {
        recordList.add(new ProtoRecord(insertion.timestamp.system_time_sec,
                                       ProtoRecord.RecordType.G4_INSERTION, insertion.toByteArray()));
      }
    }
    int numSaved = 0;
    for (ProtoRecord record : recordList) {
      if (!record.existsInDatabase()) {
        record.save();
        numSaved++;
      }
    }
    return numSaved;
  }
}
