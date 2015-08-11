package com.nightscout.core.upload.converters;

import com.mongodb.BasicDBObject;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.utils.DexcomG4Utils;

import net.tribe7.common.base.Function;

import org.joda.time.DateTime;

public class MongoConverters {

  public Function<SensorGlucoseValue, BasicDBObject> sensorGlucoseValueToMongo() {
    return new Function<SensorGlucoseValue, BasicDBObject>() {
      @Override
      public BasicDBObject apply(SensorGlucoseValue sensorGlucoseValue) {
        BasicDBObject output = new BasicDBObject();
        output.put("device", "dexcom");
        output.put("sysTime", sensorGlucoseValue.timestamp.system_time_sec);
        output.put("date", sensorGlucoseValue.timestamp.wall_time_sec * 1000);
        output.put("dateString", new DateTime(sensorGlucoseValue.timestamp.wall_time_sec * 1000).toString());
        output.put("sgv", sensorGlucoseValue.glucose_mgdl);
        output.put("direction", DexcomG4Utils.getFriendlyTrendName(sensorGlucoseValue.trend));
        output.put("type", "sgv");
        return output;
      }
    };
  }

  protected BasicDBObject toBasicDBObjectSensor(GlucoseDataSet glucoseDataSet, BasicDBObject output) {
    output.put("filtered", glucoseDataSet.getFiltered());
    output.put("unfiltered", glucoseDataSet.getUnfiltered());
    output.put("rssi", glucoseDataSet.getRssi());
    output.put("noise", glucoseDataSet.getNoise());
    return output;
  }
}
