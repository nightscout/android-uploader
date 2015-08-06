package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.MeterEntry;
import com.nightscout.core.model.v2.G4Timestamp;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.utils.GlucoseReading;

import net.tribe7.common.base.Function;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
        setRecordType();
    }

    public MeterRecord(int meterBgMgdl, int meterTime, long displayTime, long systemTime, long rcvrTime, long refTime) {
        super(displayTime, systemTime, rcvrTime, refTime);
        this.reading = new GlucoseReading(meterBgMgdl, GlucoseUnit.MGDL);
        this.meterTime = meterTime;
        setRecordType();
    }



    @Override
    protected void setRecordType() {
        this.recordType = "meter";
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

    public static Function<MeterRecord, ManualMeterEntry> v2ModelConverter(
        final Function<Long, Long> wallTimeConverter) {
        return new Function<MeterRecord, ManualMeterEntry>() {
            @Override
            public ManualMeterEntry apply(MeterRecord input) {
                return new ManualMeterEntry.Builder()
                    .entered_blood_glucose_mgdl(input.getBgMgdl())
                    .meter_time(input.getMeterTime())
                    .timestamp(new G4Timestamp.Builder()
                                   .system_time_sec(input.getRawSystemTimeSeconds())
                                   .display_time_sec(input.getRawDisplayTimeSeconds())
                                   .wall_time_sec(wallTimeConverter.apply(input.getRawSystemTimeSeconds())).build())
                    .build();
            }
        };
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
