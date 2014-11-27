package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SensorRecord extends GenericTimestampRecord {

    public static final int RECORD_SIZE = 19;
    private int unfiltered;
    private int filtered;
    private int rssi;
    private int OFFSET_UNFILTERED = 8;
    private int OFFSET_FILTERED = 12;
    private int OFFSET_RSSI = 16;

    public SensorRecord(byte[] packet) throws InvalidRecordLengthException {
        super(packet);
        if (packet.length != RECORD_SIZE) {
            try {
                throw new InvalidRecordLengthException("Unexpected record size: "+packet.length+". Expected size: "+RECORD_SIZE+". Unparsed record: "+new String(packet,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // nom
            }
        }

        unfiltered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_UNFILTERED);
        filtered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_FILTERED);
        rssi = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(OFFSET_RSSI);
    }

    public long getUnfiltered() {
        return unfiltered;
    }

    public long getFiltered() {
        return filtered;
    }

    public int getRssi() {
        return rssi;
    }
}
