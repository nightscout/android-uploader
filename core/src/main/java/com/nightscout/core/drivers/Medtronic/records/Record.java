package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class Record {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected byte recordOp;
    protected int totalSize = -1;
    protected byte timestampSize = 0;
    protected int bodySize = 0;
    protected byte headerSize = 2;
    protected PumpModel model = PumpModel.UNSET;
    protected String recordTypeName = this.getClass().getSimpleName();

    public Record(byte[] data, PumpModel model) {
        recordOp = data[0];
        this.model = model;
    }

    public int getSize() {
        if (totalSize == -1) {
            totalSize = headerSize + timestampSize + bodySize;
        }
        return totalSize;
    }

    public byte getRecordOp() {
        return recordOp;
    }

    public void logRecord() {
        log.info("Unparsed {}", recordTypeName);
    }

    abstract protected void decode(byte[] data);
}