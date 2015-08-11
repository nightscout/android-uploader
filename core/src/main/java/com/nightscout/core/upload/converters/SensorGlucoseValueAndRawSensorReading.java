package com.nightscout.core.upload.converters;

import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;

public class SensorGlucoseValueAndRawSensorReading {
  public RawSensorReading raw_sensor_reading;
  public SensorGlucoseValue sensor_glucose_value;

  public SensorGlucoseValueAndRawSensorReading() {}
  public SensorGlucoseValueAndRawSensorReading(SensorGlucoseValue sensorGlucoseValue, RawSensorReading rawSensorReading) {
    this.sensor_glucose_value = sensorGlucoseValue;
    this.raw_sensor_reading = rawSensorReading;
  }
}
