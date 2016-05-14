package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.G4Noise;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.utils.GlucoseReading;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;

public class EGVRecord extends GenericTimestampRecord {
    public final static int G4_RECORD_SIZE = 12;
    public final static int G5_RECORD_SIZE = 22;
    private GlucoseReading reading;
    private TrendArrow trend;
    private G4Noise noiseMode;
    private int recordVersion = 0;

    public EGVRecord(byte[] packet, int recordVersion) {
        super(packet);
        this.recordVersion = recordVersion;
        if (recordVersion < 4 && packet.length != G4_RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + G4_RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        else if (recordVersion >= 4 && packet.length != G5_RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + G5_RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        int bGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8) & Constants.EGV_VALUE_MASK;
        reading = new GlucoseReading(bGValue, GlucoseUnit.MGDL);
        byte trendAndNoise;
        if(recordVersion < 4) {
            trendAndNoise = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).get(10);
        }
        else {
            //A G5 has 'systemTimeSeconds' (4 bytes), 'transmitterTimeSeconds' (4 bytes), and 'filteredRateByte' (1 byte) before the trendArrowAndNoiseMode byte
            trendAndNoise = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).get(19);
        }

        int trendValue = trendAndNoise & Constants.EGV_TREND_ARROW_MASK;
        byte noiseValue = (byte) ((trendAndNoise & Constants.EGV_NOISE_MASK) >> 4);
        trend = TrendArrow.values()[trendValue];
        noiseMode = G4Noise.values()[noiseValue];
    }

    public EGVRecord(int bGValueMgdl, TrendArrow trend, Date displayTime, Date systemTime, G4Noise noise) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(bGValueMgdl, GlucoseUnit.MGDL);
        this.trend = trend;
        this.noiseMode = noise;
    }

    public EGVRecord(int bGValueMgdl, TrendArrow trend, long displayTime, long systemTime, G4Noise noise) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(bGValueMgdl, GlucoseUnit.MGDL);
        this.trend = trend;
        this.noiseMode = noise;
    }

    public EGVRecord(SensorGlucoseValueEntry sgv) {
        super(sgv.disp_timestamp_sec, sgv.sys_timestamp_sec);
        this.reading = new GlucoseReading(sgv.sgv_mgdl, GlucoseUnit.MGDL);
        this.trend = TrendArrow.values()[sgv.trend.ordinal()];
        this.noiseMode = sgv.noise;
    }

    public int getBgMgdl() {
        return reading.asMgdl();
    }

    public TrendArrow getTrend() {
        return trend;
    }

    public G4Noise getNoiseMode() {
        return noiseMode;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("sgv", getBgMgdl());
        obj.put("date", getDisplayTime());
        return obj;
    }

    @Override
    public SensorGlucoseValueEntry toProtobuf() {
        SensorGlucoseValueEntry.Builder builder = new SensorGlucoseValueEntry.Builder();
        return builder.sys_timestamp_sec(rawSystemTimeSeconds)
                .disp_timestamp_sec(rawDisplayTimeSeconds)
                .sgv_mgdl(reading.asMgdl())
                .trend(trend.toProtobuf())
                .noise(noiseMode)
                .build();
    }

    public static List<SensorGlucoseValueEntry> toProtobufList(List<EGVRecord> list) {
        return toProtobufList(list, SensorGlucoseValueEntry.class);
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
