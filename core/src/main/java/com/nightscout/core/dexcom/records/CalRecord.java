package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CalRecord extends GenericTimestampRecord {
    private static final Logger LOG = LoggerFactory.getLogger(CalRecord.class);
    public final static int RECORD_SIZE = 147;
    public final static int RECORD_V2_SIZE = 248;
    private double slope;
    private double intercept;
    private double scale;
    private int[] unk = new int[3];
    private double decay;
    private int  numRecords;
    private CalSubrecord[] calSubrecords = new CalSubrecord[12];
    private int SUB_LEN = 17;

    public CalRecord(byte[] packet) throws InvalidRecordLengthException {
        super(packet);
        if (packet.length != RECORD_SIZE && packet.length != RECORD_V2_SIZE) {
            try {
                throw new InvalidRecordLengthException("Unexpected record size: "+packet.length+". Expected size: "+RECORD_SIZE+". Unparsed record: "+new String(packet,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // nom
            }
        }
        slope = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(8);
        intercept = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(16);
        scale = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(24);
        unk[0] = packet[32];
        unk[1] = packet[33];
        unk[2] = packet[34];
        decay = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(35);
        numRecords = packet[43];

        long displayTimeOffset = Seconds.secondsBetween(
                new DateTime(getSystemTime()),
                new DateTime(getDisplayTime())).toStandardDuration().getMillis();
        int start = 44;
        for (int i = 0; i < numRecords; i++) {
            LOG.debug("Loop #"+i);
            byte[] temp = new byte[SUB_LEN];
            System.arraycopy(packet, start, temp, 0, temp.length);
            calSubrecords[i] = new CalSubrecord(temp, displayTimeOffset);
            start += SUB_LEN;
        }
    }

    public double getSlope() {
        return slope;
    }

    public double getIntercept() {
        return intercept;
    }

    public double getScale() {
        return scale;
    }

    public int[] getUnk() {
        return unk;
    }

    public double getDecay() {
        return decay;
    }

    public int getNumRecords() {
        return numRecords;
    }

    public CalSubrecord[] getCalSubrecords() {
        return calSubrecords;
    }
}
