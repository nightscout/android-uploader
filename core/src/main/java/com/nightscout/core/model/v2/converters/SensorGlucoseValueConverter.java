package com.nightscout.core.model.v2.converters;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.v2.SensorGlucoseValue;

import net.tribe7.common.base.Function;

import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorGlucoseValueConverter implements Function<SensorGlucoseValue, JSONObject> {

  private static final Logger log = LoggerFactory.getLogger(SensorGlucoseValueConverter.class);

  private SensorGlucoseValueConverter() {}

  public static SensorGlucoseValueConverter instance() {
    return new SensorGlucoseValueConverter();
  }

  @Override
  public JSONObject apply(SensorGlucoseValue glucoseValue) {
      JSONObject obj = new JSONObject();
      try {
        obj.put("sgv", glucoseValue.glucose_mgdl);
        obj.put("date",
                Utils.receiverTimeToDateTime(glucoseValue.timestamp.display_time_sec).toString(
                    ISODateTimeFormat.dateTime()));
      } catch (JSONException e) {
        log.error("Error creating JSON with SensorGlucoseValue {}.", glucoseValue, e);
      }
      return obj;
    }

}
