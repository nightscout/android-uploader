package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.CRC16;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.RecordType;
import com.nightscout.core.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class PageHeader {
    public static final int HEADER_SIZE=28;
    protected final int FIRSTRECORDINDEX_OFFSET=0;
    protected final int NUMRECS_OFFSET=4;
    protected final int RECTYPE_OFFSET=8;
    protected final int REV_OFFSET=9;
    protected final int PAGENUMBER_OFFSET=10;
    protected final int RESERVED2_OFFSET=14;
    protected final int RESERVED3_OFFSET=18;
    protected final int RESERVED4_OFFSET=22;

    protected int firstRecordIndex;
    protected int numOfRecords;
    protected RecordType recordType;
    protected byte revision;
    protected int pageNumber;
    protected int reserved2;
    protected int reserved3;
    protected int reserved4;
    protected byte[] crc=new byte[2];


    public PageHeader(byte[] packet) {
        if (packet.length < HEADER_SIZE){
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + HEADER_SIZE + ". Unparsed record: " +
                    Utils.bytesToHex(packet));
        }
        firstRecordIndex = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
                .getInt(FIRSTRECORDINDEX_OFFSET);
        numOfRecords = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(NUMRECS_OFFSET);
        recordType = RecordType.values()[packet[RECTYPE_OFFSET]];
        revision = packet[REV_OFFSET];
        pageNumber = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
                .getInt(PAGENUMBER_OFFSET);
        reserved2 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED2_OFFSET);
        reserved3 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED3_OFFSET);
        reserved4 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED4_OFFSET);
        System.arraycopy(packet, HEADER_SIZE-Constants.CRC_LEN, crc, 0, Constants.CRC_LEN);
        byte[] crc_calc = CRC16.calculate(packet, 0, HEADER_SIZE - Constants.CRC_LEN);
        if (!Arrays.equals(this.crc, crc_calc)) {
            throw new CRCFailError("CRC check failed. Was:" + Utils.bytesToHex(this.crc) +
                    " Expected: " + Utils.bytesToHex(crc_calc));
        }

    }

    public byte getRevision() {
        return revision;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public int getFirstRecordIndex() {
        return firstRecordIndex;
    }

    public int getNumOfRecords() {
        return numOfRecords;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getReserved2() {
        return reserved2;
    }

    public int getReserved3() {
        return reserved3;
    }

    public int getReserved4() {
        return reserved4;
    }
}
