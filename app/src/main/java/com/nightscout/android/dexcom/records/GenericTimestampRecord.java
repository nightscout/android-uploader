package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class GenericTimestampRecord {

    private final int OFFSET_SYS_TIME = 0;
    private final int OFFSET_DISPLAY_TIME = 4;
    private Date systemTime;
    private int systemTimeSeconds;
    private Date displayTime;

    public GenericTimestampRecord(byte[] packet) {
        systemTimeSeconds = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_SYS_TIME);
        systemTime = Utils.receiverTimeToDate(systemTimeSeconds);
        int dt = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_DISPLAY_TIME);
        displayTime = Utils.receiverTimeToDate(dt);
    }

    public GenericTimestampRecord(Date displayTime, Date systemTime){
        this.displayTime=displayTime;
        this.systemTime=systemTime;
    }

    public GenericTimestampRecord(long displayTime, long systemTime){
        this.displayTime=new Date(displayTime);
        this.systemTime=new Date(systemTime);
    }


    public Date getSystemTime() {
        return systemTime;
    }

    public int getSystemTimeSeconds() {
        return systemTimeSeconds;
    }

    public Date getDisplayTime() {
        return displayTime;
    }

    public long getDisplayTimeSeconds() {
        return displayTime.getTime();
    }

}
