package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.utils.GlucoseReading;

import java.util.Date;

public class GlucoseDataSet {

    private Date systemTime;
    private Date displayTime;
    private GlucoseReading reading;
    private TrendArrow trend;
    private int noise;
    private long unfiltered;
    private long filtered;
    private int rssi;

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        // TODO check times match between record
        systemTime = egvRecord.getSystemTime();
        displayTime = egvRecord.getDisplayTime();
        reading = egvRecord.getReading();
        trend = egvRecord.getTrend();
        noise = egvRecord.getNoiseMode().ordinal();
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

    public int getBgMgdl() {
        return reading.asMgdl();
    }

    public GlucoseReading getReading() {
        return reading;
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
