package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.utils.GlucoseReading;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;

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

    public MeterRecord(int meterBgMgdl, int meterTime, long displayTime, int systemTime) {
        super(displayTime, systemTime);
        this.reading = new GlucoseReading(meterBgMgdl, GlucoseUnit.MGDL);
        this.meterTime = meterTime;
    }

    public MeterRecord(MeterEntry meter) {
        super(meter.disp_timestamp_sec, meter.sys_timestamp_sec);
        this.reading = new GlucoseReading(meter.meter_bg_mgdl, GlucoseUnit.MGDL);
        this.meterTime = meter.meter_time;
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

    @Override
    public MeterEntry toProtobuf() {
        MeterEntry.Builder builder = new MeterEntry.Builder();
        return builder.sys_timestamp_sec(rawSystemTimeSeconds)
                .disp_timestamp_sec(rawDisplayTimeSeconds)
                .meter_time(meterTime)
                .meter_bg_mgdl(reading.asMgdl())
                .build();
    }

    public static List<MeterEntry> toProtobufList(List<MeterRecord> list) {
        return toProtobufList(list, MeterEntry.class);
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
}
