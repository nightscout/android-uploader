package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;
import com.squareup.wire.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

abstract public class GenericTimestampRecord {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final int OFFSET_SYS_TIME = 0;
    protected final int OFFSET_DISPLAY_TIME = 4;
    protected Date systemTime;
    protected long rawSystemTimeSeconds;
    protected Date displayTime;
    protected long rawDisplayTimeSeconds;

    public GenericTimestampRecord(byte[] packet) {
        rawSystemTimeSeconds = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_SYS_TIME);
        systemTime = Utils.receiverTimeToDate(rawSystemTimeSeconds);
        rawDisplayTimeSeconds = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_DISPLAY_TIME);
        displayTime = Utils.receiverTimeToDate(rawDisplayTimeSeconds);
    }

    public GenericTimestampRecord(Date displayTime, Date systemTime) {
        this.displayTime = displayTime;
        this.systemTime = systemTime;
    }

    public GenericTimestampRecord(long rawDisplayTimeSeconds, long rawSystemTimeSeconds) {
        this.rawDisplayTimeSeconds = rawDisplayTimeSeconds;
        this.rawSystemTimeSeconds = rawSystemTimeSeconds;
        this.systemTime = Utils.receiverTimeToDate(rawSystemTimeSeconds);
        this.displayTime = Utils.receiverTimeToDate(rawDisplayTimeSeconds);
    }

    public Date getSystemTime() {
        return systemTime;
    }

    public long getRawSystemTimeSeconds() {
        return rawSystemTimeSeconds;
    }

    public Date getDisplayTime() {
        return displayTime;
    }

    public long getRawDisplayTimeSeconds() {
        return rawDisplayTimeSeconds;
    }

    abstract protected Message toProtobuf();

    public static <T extends Message, S extends GenericTimestampRecord> List<T> toProtobufList(
            List<S> list, Class<T> clazz) {
        List<T> results = new ArrayList<>();

        for (GenericTimestampRecord record : list) {
            results.add(clazz.cast(record.toProtobuf()));
        }
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericTimestampRecord that = (GenericTimestampRecord) o;

        if (rawDisplayTimeSeconds != that.rawDisplayTimeSeconds) return false;
        if (rawSystemTimeSeconds != that.rawSystemTimeSeconds) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (rawSystemTimeSeconds ^ (rawSystemTimeSeconds >>> 32));
        result = 31 * result + (int) (rawDisplayTimeSeconds ^ (rawDisplayTimeSeconds >>> 32));
        return result;
    }
}
