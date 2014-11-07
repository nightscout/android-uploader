package com.nightscout.android.dexcom.records;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MeterRecord extends GenericTimestampRecord {

    private int meterBG;
    private int meterTime;

    public MeterRecord(byte[] packet) {
        super(packet);
        meterBG = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        meterTime = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(10);
    }

    public MeterRecord(int meterBG, int meterTime,long displayTime, long systemTime) {
        super(displayTime,systemTime);
        this.meterBG=meterBG;
        this.meterTime=meterTime;
    }

    public int getMeterBG() {
        return meterBG;
    }

    public int getMeterTime() {
        return meterTime;
    }
}
