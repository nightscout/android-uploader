package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PageHeader {
    protected final int FIRSTRECORDINDEX_OFFSET=0;
    protected final int NUMRECS_OFFSET=4;
    protected final int PAGENUMBER_OFFSET=10;
    protected final int RESERVED2_OFFSET=14;
    protected final int RESERVED3_OFFSET=18;
    protected final int RESERVED4_OFFSET=22;

    protected int firstRecordIndex;
    protected int numOfRecords;
    protected Constants.RECORD_TYPES recordType;
    protected byte revision;
    protected int pageNumber;
    protected int reserved2;
    protected int reserved3;
    protected int reserved4;


    public PageHeader(byte[] packet) {
        firstRecordIndex = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(FIRSTRECORDINDEX_OFFSET);
        numOfRecords = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(NUMRECS_OFFSET);
        pageNumber = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(PAGENUMBER_OFFSET);
        recordType = Constants.RECORD_TYPES.values()[packet[8]];
        revision = packet[9];
        reserved2 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED2_OFFSET);
        reserved3 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED3_OFFSET);
        reserved4 = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(RESERVED4_OFFSET);
    }

    public byte getRevision() {
        return revision;
    }

    public Constants.RECORD_TYPES getRecordType() {
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
