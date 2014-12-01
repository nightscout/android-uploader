package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MeterRecord extends GenericTimestampRecord {
    public final static int RECORD_SIZE = 15;
    private int meterBG;
    private int meterTime;

    public MeterRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE){
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + " record: " + Utils.bytesToHex(packet));
        }
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
