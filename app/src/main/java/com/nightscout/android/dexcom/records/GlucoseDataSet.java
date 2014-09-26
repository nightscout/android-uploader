package com.nightscout.android.dexcom.records;

public class GlucoseDataSet {

    private int bGValue;
    private String trend;
    private String trendSymbol;
    private long unfiltered;
    private long filtered;
    private int rssi;

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        bGValue = egvRecord.getBGValue();
        trend = egvRecord.getTrend();
        trendSymbol = egvRecord.getTrendSymbol();
        unfiltered = sensorRecord.getUnfiltered();
        filtered = sensorRecord.getFiltered();
        rssi = sensorRecord.getRSSI();
    }

    public int getbGValue() {
        return bGValue;
    }

    public String getTrend() {
        return trend;
    }

    public String getTrendSymbol() {
        return trendSymbol;
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
