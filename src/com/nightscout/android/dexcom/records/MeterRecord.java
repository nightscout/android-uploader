package com.nightscout.android.dexcom.records;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MeterRecord {
    private int systemTime;
    private int displayTime;
    private int meterBG;
    private int meterTime;

    public MeterRecord(byte[] packet) {
        meterBG = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        meterTime = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(10);
    }

    public int getMeterBG() {
        return meterBG;
    }

    public int getMeterTime() {
        return meterTime;
    }
}
