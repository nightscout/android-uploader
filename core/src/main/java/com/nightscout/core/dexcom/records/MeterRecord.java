package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.protobuf.G4Download;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class MeterRecord extends GenericTimestampRecord {
    public final static int RECORD_SIZE = 15;
    private int meterBG;
    private int meterTime;

    public MeterRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE){
            try {
                throw new InvalidRecordLengthException("Unexpected record size: "+packet.length+". Expected size: "+RECORD_SIZE+" record: "+new String(packet,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // nom
            }
        }
        meterBG = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        meterTime = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(10);
    }

    public MeterRecord(int meterBG, int meterTime, Date displayTime, Date systemTime){
        super(displayTime, systemTime);
        this.meterBG = meterBG;
        this.meterTime = meterTime;
    }

    public MeterRecord(int meterBG, int meterTime, long displayTime, int systemTime){
        super(displayTime, systemTime);
        this.meterBG = meterBG;
        this.meterTime = meterTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)){ return false;}

        MeterRecord that = (MeterRecord) o;

        if (meterBG != that.meterBG) return false;
        if (meterTime != that.meterTime) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + meterBG;
        result = 31 * result + meterTime;
        return result;
    }

    public int getMeterBG() {
        return meterBG;
    }

    public int getMeterTime() {
        return meterTime;
    }

    @Override
    public G4Download.CookieMonsterG4Meter toProtobuf() {
        G4Download.CookieMonsterG4Meter.Builder builder = G4Download.CookieMonsterG4Meter.newBuilder();
        return builder.setTimestampSec(rawSystemTimeSeconds)
                .setMeterTime(meterTime)
                .setMeterBgMgdl(meterBG)
                .build();
    }
}
