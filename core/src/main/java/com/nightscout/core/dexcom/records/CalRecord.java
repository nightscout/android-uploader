package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.protobuf.G4Download;
import com.nightscout.core.dexcom.Utils;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

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
    private CalSubrecord[] calSubrecords;
    private int SUB_LEN = 17;

    public CalRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE && packet.length != RECORD_V2_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        slope = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(8);
        intercept = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(16);
        scale = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(24);
        unk[0] = packet[32];
        unk[1] = packet[33];
        unk[2] = packet[34];
        decay = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getDouble(35);
        numRecords = packet[43];
        calSubrecords = new CalSubrecord[numRecords];

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

    public CalRecord(double intercept, double slope, double scale, double decay, Date displayTime, Date systemTime, CalSubrecord[] subrecord) {
        super(displayTime, systemTime);
        this.intercept = intercept;
        this.slope = slope;
        this.scale = scale;
        this.decay = decay;
        this.numRecords = subrecord.length;
        this.calSubrecords = subrecord;
    }

    public CalRecord(double intercept, double slope, double scale, double decay, long displayTime, int systemTime, CalSubrecord[] subrecord) {
        super(displayTime, systemTime);
        this.intercept = intercept;
        this.slope = slope;
        this.scale = scale;
        this.decay = decay;
        this.numRecords = subrecord.length;
        this.calSubrecords = subrecord;
    }

    @Override
    public G4Download.CookieMonsterG4Cal toProtobuf() {
        G4Download.CookieMonsterG4Cal.Builder builder = G4Download.CookieMonsterG4Cal.newBuilder();
        return builder.setTimestampSec(rawSystemTimeSeconds)
                .setIntercept(intercept)
                .setScale(scale)
                .setSlope(slope)
                .build();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CalRecord calRecord = (CalRecord) o;

        if (Double.compare(calRecord.decay, decay) != 0) return false;
        if (Double.compare(calRecord.intercept, intercept) != 0) return false;
        if (numRecords != calRecord.numRecords) return false;
        if (Double.compare(calRecord.scale, scale) != 0) return false;
        if (Double.compare(calRecord.slope, slope) != 0) return false;
        if (!Arrays.equals(calSubrecords, calRecord.calSubrecords)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(slope);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(intercept);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(scale);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(decay);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + numRecords;
        result = 31 * result + Arrays.hashCode(calSubrecords);
        return result;
    }
}
