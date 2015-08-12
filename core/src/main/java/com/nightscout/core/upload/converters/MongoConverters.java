package com.nightscout.core.upload.converters;

import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.G4Timestamp;
import com.nightscout.core.model.v2.Insertion;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.utils.DexcomG4Utils;

import net.tribe7.common.base.Function;

import org.bson.Document;
import org.joda.time.DateTime;

public class MongoConverters {

  private static void fillTimestamp(G4Timestamp timestamp, Document dbObject) {
    if (timestamp == null) {
      return;
    }
    DateTime wallTime = new DateTime(timestamp.wall_time_sec * 1000);
    dbObject.put("sysTime", timestamp.system_time_sec);
    dbObject.put("date", wallTime.getMillis());
    dbObject.put("dateString", wallTime.toString());
  }

  public static Function<SensorGlucoseValueAndRawSensorReading, Document> sensorReadingConverter() {
    return new Function<SensorGlucoseValueAndRawSensorReading, Document>() {
      @Override
      public Document apply(SensorGlucoseValueAndRawSensorReading reading) {
        Document output = new Document();
        output.put("device", "dexcom");
        if (reading.sensor_glucose_value != null) {
          SensorGlucoseValue sensorGlucoseValue = reading.sensor_glucose_value;
          fillTimestamp(sensorGlucoseValue.timestamp, output);
          output.put("sgv", sensorGlucoseValue.glucose_mgdl);
          output.put("direction", DexcomG4Utils.getFriendlyTrendName(sensorGlucoseValue.trend));
          output.put("noise", sensorGlucoseValue.noise.getValue());
          output.put("type", "sgv");
        }
        if (reading.raw_sensor_reading != null) {
          RawSensorReading rawSensorReading = reading.raw_sensor_reading;
          output.put("filtered", rawSensorReading.filtered);
          output.put("unfiltered", rawSensorReading.unfiltered);
          output.put("rssi", rawSensorReading.rssi);
        }
        return output;
      }
    };
  }

  public static Function<ManualMeterEntry, Document> manualMeterEntryConverter() {
    return new Function<ManualMeterEntry, Document>() {
      @Override
      public Document apply(ManualMeterEntry input) {
        Document output = new Document();
        if (input == null) {
          return output;
        }
        output.put("device", "dexcom");
        output.put("type", "mbg");
        fillTimestamp(input.timestamp, output);
        output.put("mbg", input.entered_blood_glucose_mgdl);
        return output;
      }
    };
  }

  public static Function<Calibration, Document> calibrationConverter() {
    return new Function<Calibration, Document>() {
      @Override
      public Document apply(Calibration input) {
        Document output = new Document();
        if (input == null) {
          return output;
        }
        output.put("device", "dexcom");
        fillTimestamp(input.timestamp, output);
        output.put("slope", input.slope);
        output.put("intercept", input.intercept);
        output.put("scale", input.scale);
        output.put("type", "cal");
        return output;
      }
    };
  }

  public static Function<Insertion, Document> insertionConverter() {
    return new Function<Insertion, Document>() {
      @Override
      public Document apply(Insertion input) {
        Document output = new Document();
        if (input == null) {
          return output;
        }
        fillTimestamp(input.timestamp, output);
        output.put("state", input.state.name());
        output.put("type", "ins");
        return output;
      }
    };
  }

  /*
  private BasicDBObject toBasicDBObject(AbstractUploaderDevice deviceStatus, int rcvrBat) {
    BasicDBObject output = new BasicDBObject();
    output.put("uploaderBattery", deviceStatus.getBatteryLevel());
    output.put("receiverBattery", rcvrBat);
    output.put("device", "dexcom");
    output.put("created_at", new Date());
    return output;
  }
  */

}
