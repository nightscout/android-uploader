package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Trend;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class SensorRecord extends GenericTimestampRecord {

    private long unfiltered;
    private long filtered;
    private int rssi;
    private int OFFSET_UNFILTERED = 8;
    private int OFFSET_FILTERED = 12;
    private int OFFSET_RSSI = 16;

    public SensorRecord(byte[] packet) {
        super(packet);
        unfiltered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_UNFILTERED);
        filtered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_FILTERED);
        rssi = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(OFFSET_RSSI);
    }

    public SensorRecord(long unfiltered,long filtered,int rssi, long displayTime, long systemTime){
        super(displayTime,systemTime);
        this.unfiltered=unfiltered;
        this.filtered=filtered;
        this.rssi=rssi;
    }

    public long getUnfiltered() {
        return unfiltered;
    }

    public long getFiltered() {
        return filtered;
    }

    public int getRSSI() {
        return rssi;
    }
}
