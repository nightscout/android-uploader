package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.SensorEntry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class SensorRecord extends GenericTimestampRecord {
    public static final int RECORD_SIZE = 19;
    private long unfiltered;
    private long filtered;
    private int rssi;
    private int OFFSET_UNFILTERED = 8;
    private int OFFSET_FILTERED = 12;
    private int OFFSET_RSSI = 16;

    public SensorRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        unfiltered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_UNFILTERED);
        filtered = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_FILTERED);
        rssi = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(OFFSET_RSSI);
    }

    public SensorRecord(int filtered, int unfiltered, int rssi, long displayTime, int systemTime) {
        super(displayTime, systemTime);
        this.filtered = filtered;
        this.unfiltered = unfiltered;
        this.rssi = rssi;
    }

    public SensorRecord(SensorEntry sensor) {
        super(sensor.disp_timestamp_sec, sensor.sys_timestamp_sec);
        this.filtered = sensor.filtered;
        this.unfiltered = sensor.unfiltered;
        this.rssi = sensor.rssi;
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

    @Override
    public SensorEntry toProtobuf() {
        SensorEntry.Builder builder = new SensorEntry.Builder();
        return builder.sys_timestamp_sec(rawSystemTimeSeconds)
                .disp_timestamp_sec(rawDisplayTimeSeconds)
                .rssi(rssi)
                .filtered(filtered)
                .unfiltered(unfiltered)
                .build();

    }

    public static List<SensorEntry> toProtobufList(List<SensorRecord> list) {
        return toProtobufList(list, SensorEntry.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SensorRecord that = (SensorRecord) o;

        if (filtered != that.filtered) return false;
        if (rssi != that.rssi) return false;
        if (unfiltered != that.unfiltered) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (unfiltered ^ (unfiltered >>> 32));
        result = 31 * result + (int) (filtered ^ (filtered >>> 32));
        result = 31 * result + rssi;
        return result;
    }
}
