package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.TrendArrow;

import java.util.Date;

public class GlucoseDataSet {

  private Date systemTime;
  private Date displayTime;
  private int bGValue;
  private TrendArrow trend;
  private int noise;
  private long unfiltered;
  private long filtered;
  private int rssi;

  public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
    // TODO check times match between record
    systemTime = egvRecord.getSystemTime();
    displayTime = egvRecord.getDisplayTime();
    bGValue = egvRecord.getBGValue();
    trend = egvRecord.getTrend();
    noise = egvRecord.getNoiseMode().getValue();
    unfiltered = sensorRecord.getUnfiltered();
    filtered = sensorRecord.getFiltered();
    rssi = sensorRecord.getRssi();
  }

  public Date getSystemTime() {
    return systemTime;
  }

  public Date getDisplayTime() {
    return displayTime;
  }

  public int getBGValue() {
    return bGValue;
  }

  public TrendArrow getTrend() {
    return trend;
  }

  public String getTrendSymbol() {
    return trend.symbol();
  }

  public int getNoise() {
    return noise;
  }

  public long getUnfiltered() {
    return unfiltered;
  }

  public long getFiltered() {
    return filtered;
  }

  public int getRssi() {
    return rssi;
  }
}
