package com.nightscout.core.dexcom.records;

import com.google.common.base.Optional;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.protobuf.CookieMonsterG4Meter;
import com.nightscout.core.protobuf.GlucoseUnit;
import com.nightscout.core.utils.GlucoseReading;
import com.squareup.wire.Wire;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class MeterRecord extends GenericTimestampRecord {
    public final static int RECORD_SIZE = 15;
    private int meterTime;
    private GlucoseReading reading;

    public MeterRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + " record: " + Utils.bytesToHex(packet));
        }
        int meterBG = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        reading = new GlucoseReading(meterBG, GlucoseUnit.MGDL);
        meterTime = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(10);
    }

    public MeterRecord(int meterBgMgdl, int meterTime, Date displayTime, Date systemTime) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(meterBgMgdl, GlucoseUnit.MGDL);
        this.meterTime = meterTime;
    }

    public MeterRecord(int meterBgMgdl, int meterTime, long displayTime, long systemTime) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(meterBgMgdl, GlucoseUnit.MGDL);
        this.meterTime = meterTime;
    }

    public MeterRecord(int meterBgMgdl, int meterTime, long systemTime) {
        super(systemTime);
        this.reading = new GlucoseReading(meterBgMgdl, GlucoseUnit.MGDL);
        this.meterTime = meterTime;
    }


    public GlucoseReading getMeterBG() {
        return reading;
    }

    public int getBgMgdl() {
        return reading.asMgdl();
    }

    public int getMeterTime() {
        return meterTime;
    }

    public CookieMonsterG4Meter toProtobuf() {
        CookieMonsterG4Meter.Builder builder = new CookieMonsterG4Meter.Builder();
        return builder.timestamp_sec(rawSystemTimeSeconds)
                .meter_time(meterTime)
                .meter_bg_mgdl(reading.asMgdl())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MeterRecord that = (MeterRecord) o;

        if (meterTime != that.meterTime) return false;
        if (!reading.equals(that.reading)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + meterTime;
        result = 31 * result + reading.hashCode();
        return result;
    }

    public Optional<MeterRecord> fromProtoBuf(byte[] byteArray) {
        try {
            Wire wire = new Wire();
            CookieMonsterG4Meter record = wire.parseFrom(byteArray, CookieMonsterG4Meter.class);
            return Optional.of(new MeterRecord(record.meter_bg_mgdl, record.meter_time, record.timestamp_sec));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.absent();
    }
}
