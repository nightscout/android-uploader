package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.protobuf.CookieMonsterG4SGV;
import com.nightscout.core.protobuf.GlucoseUnit;
import com.nightscout.core.protobuf.Noise;
import com.nightscout.core.utils.GlucoseReading;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class EGVRecord extends GenericTimestampRecord {
    public final static int RECORD_SIZE = 12;
    private GlucoseReading reading;
    private TrendArrow trend;
    private Noise noiseMode;

    public EGVRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        int bGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8) & Constants.EGV_VALUE_MASK;
        reading = new GlucoseReading(bGValue, GlucoseUnit.MGDL);
        byte trendAndNoise = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).get(10);
        int trendValue = trendAndNoise & Constants.EGV_TREND_ARROW_MASK;
        byte noiseValue = (byte) ((trendAndNoise & Constants.EGV_NOISE_MASK) >> 4);
        trend = TrendArrow.values()[trendValue];
        noiseMode = Noise.values()[noiseValue];
    }

    public EGVRecord(int bGValueMgdl, TrendArrow trend, Date displayTime, Date systemTime, Noise noise) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(bGValueMgdl, GlucoseUnit.MGDL);
        this.trend = trend;
        this.noiseMode = noise;
    }

    public EGVRecord(int bGValueMgdl, TrendArrow trend, long displayTime, int systemTime, Noise noise) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(bGValueMgdl, GlucoseUnit.MGDL);
        this.trend = trend;
        this.noiseMode = noise;
    }

    public int getBgMgdl() {
        return reading.asMgdl();
    }

    public TrendArrow getTrend() {
        return trend;
    }

    public Noise getNoiseMode() {
        return noiseMode;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("sgv", getBgMgdl());
        obj.put("date", getDisplayTime());
        return obj;
    }

    public CookieMonsterG4SGV toProtobuf() {
        CookieMonsterG4SGV.Builder builder = new CookieMonsterG4SGV.Builder();
        return builder.timestamp_sec(rawSystemTimeSeconds)
                .sgv_mgdl(reading.asMgdl())
                .trend(trend.toProtobuf())
                .noise(noiseMode)
                .build();
    }

    public GlucoseReading getReading() {
        return reading;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EGVRecord egvRecord = (EGVRecord) o;

        if (noiseMode != egvRecord.noiseMode) return false;
        if (!reading.equals(egvRecord.reading)) return false;
        if (trend != egvRecord.trend) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + reading.hashCode();
        result = 31 * result + trend.hashCode();
        result = 31 * result + noiseMode.hashCode();
        return result;
    }
}
