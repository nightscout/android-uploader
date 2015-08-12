package com.nightscout.core.upload.converters;

import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.G4Timestamp;
import com.nightscout.core.model.v2.Insertion;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.utils.DexcomG4Utils;

import net.tribe7.common.base.Function;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public final class RestV1Converters {

  private RestV1Converters() {}

  public static Function<SensorGlucoseValueAndRawSensorReading, JSONObject> sensorReadingConverter() {
    return new Function<SensorGlucoseValueAndRawSensorReading, JSONObject>() {
      @Override
      public JSONObject apply(SensorGlucoseValueAndRawSensorReading record) {
        JSONObject json = new JSONObject();
        try {
          json.put("device", "dexcom");
          json.put("type", "sgv");
          if (record.sensor_glucose_value != null) {
            final SensorGlucoseValue sensorGlucoseValue = record.sensor_glucose_value;
            fillTimestamp(sensorGlucoseValue.timestamp, json);
            json.put("sgv", sensorGlucoseValue.glucose_mgdl);
            json.put("direction", DexcomG4Utils.getFriendlyTrendName(sensorGlucoseValue.trend));
            json.put("noise", sensorGlucoseValue.noise.getValue());
          }
          if (record.raw_sensor_reading != null) {
            final RawSensorReading rawSensorReading = record.raw_sensor_reading;
            json.put("filtered", rawSensorReading.filtered);
            json.put("unfiltered", rawSensorReading.unfiltered);
            json.put("rssi", rawSensorReading.rssi);
          }
          return json;
        } catch (JSONException e) {
          return json;
        }
      }
    };
  }

  public static Function<ManualMeterEntry, JSONObject> manualMeterEntryConverter() {
    return new Function<ManualMeterEntry, JSONObject>() {
      @Override
      public JSONObject apply(ManualMeterEntry input) {
        JSONObject json = new JSONObject();
        if (input == null) {
          return json;
        }
        try {
          json.put("device", "dexcom");
          json.put("type", "mbg");
          fillTimestamp(input.timestamp, json);
          json.put("mbg", input.entered_blood_glucose_mgdl);
        } catch (JSONException e) {
          // Empty.
        }
        return json;
      }
    };
  }

  private Function<Calibration, JSONObject> calibrationConverter() {
    return new Function<Calibration, JSONObject>() {
      @Override
      public JSONObject apply(Calibration input) {
        JSONObject json = new JSONObject();
        if (input == null) {
          return json;
        }
        try {
          fillTimestamp(input.timestamp, json);
          json.put("device", "dexcom");
          json.put("type", "cal");
          json.put("slope", input.slope);
          json.put("intercept", input.intercept);
          json.put("scale", input.scale);
        } catch (JSONException e) {
          // Empty.
        }
        return json;
      }
    };
  }

  public static Function<Insertion, JSONObject> insertionConverter() {
    return new Function<Insertion, JSONObject>() {
      @Override
      public JSONObject apply(Insertion input) {
        JSONObject json = new JSONObject();
        if (input == null) {
          return json;
        }
        try {
          fillTimestamp(input.timestamp, json);
          json.put("state", input.state.name());
        } catch (JSONException e) {
          // Empty.
        }
        return json;
      }
    };
  }

  private JSONObject toJSONObject(AbstractUploaderDevice deviceStatus, int rcvrBat)
      throws JSONException {
    JSONObject json = new JSONObject();
    json.put("uploaderBattery", deviceStatus.getBatteryLevel());
    json.put("receiverBattery", rcvrBat);
    return json;
  }

  private static void fillTimestamp(G4Timestamp timestamp, JSONObject output) throws JSONException {
    if (timestamp == null || output == null) {
      return;
    }
    DateTime wallTime = new DateTime(timestamp.wall_time_sec * 1000);
    output.put("sysTime", timestamp.system_time_sec);
    output.put("date", wallTime.getMillis());
    output.put("dateString", wallTime.toString());
  }
}
