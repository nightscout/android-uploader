package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.utils.GlucoseReading;
import com.squareup.wire.Message;

import org.joda.time.DateTime;

public class GlucoseDataSet extends GenericTimestampRecord {

    private long sensorRawSystemTime;
    private long sensorRawDisplayTime;
    private GlucoseReading reading;
    private TrendArrow trend;
    private int noise;
    private long unfiltered;
    private long filtered;
    private int rssi;

    public GlucoseDataSet(SensorGlucoseValueEntry egvRecord, long receiverTime, long referenceTime) {
        super(egvRecord.sys_timestamp_sec, egvRecord.disp_timestamp_sec, receiverTime, referenceTime);
        reading = new GlucoseReading(egvRecord.sgv_mgdl, GlucoseUnit.MGDL);
        trend = TrendArrow.values()[egvRecord.trend.ordinal()];
        noise = egvRecord.noise.ordinal();
        setRecordType();
    }

    public GlucoseDataSet(EGVRecord egvRecord) {
        super(egvRecord.getSystemTime(), egvRecord.getDisplayTime(), egvRecord.getWallTime());
        reading = egvRecord.getReading();
        trend = egvRecord.getTrend();
        noise = egvRecord.getNoiseMode().ordinal();
        setRecordType();
    }

    public boolean areRecordsMatched() {
        return Math.abs(sensorRawSystemTime - rawSystemTimeSeconds) <= 10;
    }

    public long getRawDisplayTimeEgv() {
        return rawDisplayTimeSeconds;
    }

    public long getRawSysemTimeEgv() {
        return rawSystemTimeSeconds;
    }

    public long getSensorRawSystemTime() {
        return sensorRawSystemTime;
    }

    public long getSensorRawDisplayTime() {
        return sensorRawDisplayTime;
    }

    public GlucoseDataSet(EGVRecord egvRecord, SensorRecord sensorRecord) {
        this(egvRecord);
        this.sensorRawSystemTime = sensorRecord.getRawSystemTimeSeconds();
        this.sensorRawDisplayTime = sensorRecord.getRawDisplayTimeSeconds();
        // TODO check times match between record
        unfiltered = sensorRecord.getUnfiltered();
        filtered = sensorRecord.getFiltered();
        rssi = sensorRecord.getRssi();
        setRecordType();
    }

    public GlucoseDataSet(SensorGlucoseValueEntry egvRecord, SensorEntry sensorRecord, long receiverTimestamp, long referenceTime) {
        super(egvRecord.disp_timestamp_sec, egvRecord.sys_timestamp_sec, receiverTimestamp, referenceTime);
        this.sensorRawSystemTime = sensorRecord.sys_timestamp_sec;
        this.sensorRawDisplayTime = sensorRecord.disp_timestamp_sec;
        this.reading = new GlucoseReading(egvRecord.sgv_mgdl, GlucoseUnit.MGDL);
        this.trend = TrendArrow.values()[egvRecord.trend.ordinal()];
        this.noise = egvRecord.noise.ordinal();
        this.unfiltered = sensorRecord.unfiltered;
        this.filtered = sensorRecord.filtered;
        this.rssi = sensorRecord.rssi;
        setRecordType();
    }

    public DateTime getSystemTime() {
        return systemTime;
    }

    public DateTime getDisplayTime() {
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

    public DateTime getWallTime() {
        return wallTime;
    }

    @Override
    protected Message toProtobuf() {
        return null;
    }

    @Override
    protected void setRecordType() {
        this.recordType = "sgv";
    }
}
