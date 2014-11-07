package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Trend;

import java.util.Date;

public class GlucoseDataSet {

    private Date systemTime;
    private Date displayTime;
    private int bGValue;
    private Trend trend;
    private long unfiltered;
    private long filtered;
    private int rssi;

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        // TODO check times match between record
        systemTime = egvRecord.getSystemTime();
        displayTime = egvRecord.getDisplayTime();
        bGValue = egvRecord.getBGValue();
        trend = egvRecord.getTrend();
        unfiltered = sensorRecord.getUnfiltered();
        filtered = sensorRecord.getFiltered();
        rssi = sensorRecord.getRSSI();
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

    public Trend getTrend() {
        return trend;
    }

    public String getTrendSymbol() {
        return trend.Symbol();
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
