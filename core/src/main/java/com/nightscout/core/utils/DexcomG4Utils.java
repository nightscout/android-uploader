package com.nightscout.core.utils;

import com.nightscout.core.dexcom.SpecialValue;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.G4Trend;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.model.v2.Trend;

import net.tribe7.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import static com.nightscout.core.dexcom.SpecialValue.getEGVSpecialValue;
import static net.tribe7.common.base.Preconditions.checkNotNull;

public final class DexcomG4Utils {

  private DexcomG4Utils() {}

  /**
   * Returns the time since the latest reading. If there are no readings, this defaults to 2 minutes.
   * @param g4Data downloaded g4 data.
   * @return Duration sing last reading.
   */
  public static Duration timeSinceLastReading(final G4Data g4Data) {
    Optional<SensorGlucoseValue>
        lastReadingOptional = ListUtils.lastOrEmpty(g4Data.sensor_glucose_values);
    if (lastReadingOptional.isPresent()) {
      return Duration.standardSeconds(
          g4Data.receiver_system_time_sec - g4Data.sensor_glucose_values
              .get(g4Data.sensor_glucose_values.size() - 1).timestamp.system_time_sec);
    } else {
      return Duration.standardMinutes(2);
    }
  }

  public static Interval timeSinceReading(final DateTime now, final SensorGlucoseValue reading) {
    checkNotNull(now, reading);
    return new Interval(Utils.receiverTimeToDateTime(reading.timestamp.display_time_sec), now);
  }

  public static String getFriendlyTrendName(Trend trend) {
    switch (trend) {
      case TREND_NONE:
        return "NONE";
      case DOUBLE_UP:
        return "DoubleUp";
      case SINGLE_UP:
        return "SingleUp";
      case FORTY_FIVE_UP:
        return "FortyFiveUp";
      case FLAT:
        return "Flat";
      case FORTY_FIVE_DOWN:
        return "FortyFiveDown";
      case SINGLE_DOWN:
        return "SingleDown";
      case DOUBLE_DOWN:
        return "DoubleDown";
      case NOT_COMPUTABLE:
        return "NOT COMPUTABLE";
      case RATE_OUT_OF_RANGE:
        return "OUT OF RANGE";
    }
    return "UNKNOWN";
  }

  public static String getDisplayableGlucoseValueString(SensorGlucoseValue sensorGlucoseValue,
                                                        GlucoseUnit preferredUnits) {
    Optional<SpecialValue> specialValueOptional = getEGVSpecialValue(
        sensorGlucoseValue.glucose_mgdl);
    if (specialValueOptional.isPresent()) {
      return specialValueOptional.get().toString();
    }

    return GlucoseReading.mgdl(sensorGlucoseValue.glucose_mgdl).asStr(
        preferredUnits) + getTrendSymbol(sensorGlucoseValue.trend);
  }

  private static String getTrendSymbol(Trend trend) {
    switch (trend) {
      case DOUBLE_UP:
        return "\u21C8";
      case SINGLE_UP:
        return "\u2191";
      case FORTY_FIVE_UP:
        return "\u2197";
      case FLAT:
        return "\u2192";
      case FORTY_FIVE_DOWN:
        return "\u2198";
      case SINGLE_DOWN:
        return "\u2193";
      case DOUBLE_DOWN:
        return "\21CA";
      case NOT_COMPUTABLE:
      case RATE_OUT_OF_RANGE:
      case TREND_NONE:
        return "";
    }
    return "";
  }
}
