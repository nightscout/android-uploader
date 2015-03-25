package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;
import com.squareup.wire.Message;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

abstract public class GenericTimestampRecord {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final int OFFSET_SYS_TIME = 0;
    protected final int OFFSET_DISPLAY_TIME = 4;
    protected DateTime systemTime;
    protected long rawSystemTimeSeconds;
    protected DateTime displayTime;
    protected long rawDisplayTimeSeconds;
    protected DateTime wallTime;

    public GenericTimestampRecord(byte[] packet, long rcvrTime, long refTime) {
        rawSystemTimeSeconds = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_SYS_TIME);
        systemTime = Utils.receiverTimeToDate(rawSystemTimeSeconds);
        rawDisplayTimeSeconds = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_DISPLAY_TIME);
        displayTime = Utils.receiverTimeToDate(rawDisplayTimeSeconds);
        this.wallTime = Utils.systemTimeToWallTime(rawSystemTimeSeconds, rcvrTime, refTime);
    }

    public GenericTimestampRecord(DateTime displayTime, DateTime systemTime, DateTime wallTime) {
        this.displayTime = displayTime;
        this.systemTime = systemTime;
        this.wallTime = wallTime;
    }

    public GenericTimestampRecord(long rawDisplayTimeSeconds, long rawSystemTimeSeconds, long receiverTime, long refTime) {
        this.rawDisplayTimeSeconds = rawDisplayTimeSeconds;
        this.rawSystemTimeSeconds = rawSystemTimeSeconds;
        this.systemTime = Utils.receiverTimeToDate(rawSystemTimeSeconds);
        this.displayTime = Utils.receiverTimeToDate(rawDisplayTimeSeconds);
        this.wallTime = Utils.systemTimeToWallTime(rawSystemTimeSeconds, receiverTime, refTime);
    }

    public GenericTimestampRecord(long rawSystemTimeSeconds) {
        this.rawSystemTimeSeconds = rawSystemTimeSeconds;
    }

    public DateTime getSystemTime() {
        return systemTime;
    }

    public long getRawSystemTimeSeconds() {
        return rawSystemTimeSeconds;
    }

    public DateTime getDisplayTime() {
        return displayTime;
    }

    public long getDisplayTimeSeconds() {
        return displayTime.getMillis();
    }

    public long getRawDisplayTimeSeconds() {
        return rawDisplayTimeSeconds;
    }

    public DateTime getWallTime() {
        return wallTime;
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
