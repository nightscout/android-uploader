package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.CookieMonsterG4SGV;
import com.nightscout.core.model.CookieMonsterG4Sensor;
import com.nightscout.core.model.GlucoseUnit;
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

    public GlucoseDataSet(CookieMonsterG4SGV egvRecord) {
        systemTime = Utils.receiverTimeToDate(egvRecord.sys_timestamp_sec);
        displayTime = Utils.receiverTimeToDate(egvRecord.disp_timestamp_sec);
        reading = new GlucoseReading(egvRecord.sgv_mgdl, GlucoseUnit.MGDL);
        trend = TrendArrow.values()[egvRecord.trend.ordinal()];
        noise = egvRecord.noise.ordinal();
    }

    public GlucoseDataSet(EGVRecord egvRecord) {
        systemTime = egvRecord.getSystemTime();
        displayTime = egvRecord.getDisplayTime();
        reading = egvRecord.getReading();
        trend = egvRecord.getTrend();
        noise = egvRecord.getNoiseMode().ordinal();
    }

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        this(egvRecord);
        // TODO check times match between record
        unfiltered = sensorRecord.getUnfiltered();
        filtered = sensorRecord.getFiltered();
        rssi = sensorRecord.getRssi();
    }

    public GlucoseDataSet(CookieMonsterG4SGV egvRecord, CookieMonsterG4Sensor sensorRecord) {
        this.systemTime = new Date(egvRecord.sys_timestamp_sec);
        this.displayTime = new Date(egvRecord.disp_timestamp_sec);
        this.reading = new GlucoseReading(egvRecord.sgv_mgdl, GlucoseUnit.MGDL);
        this.trend = TrendArrow.values()[egvRecord.trend.ordinal()];
        this.noise = egvRecord.noise.ordinal();
        this.unfiltered = sensorRecord.unfiltered;
        this.filtered = sensorRecord.filtered;
        this.rssi = sensorRecord.rssi;
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
