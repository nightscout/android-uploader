package com.nightscout.core.drivers.Medtronic.remote_commands;

import com.nightscout.core.drivers.Medtronic.request.TransmitPacketRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class CommandBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected byte CODE;
    protected int MAX_RECORDS;
    protected int BYTES_PER_RECORD;
    protected byte[] PARAMS;
    protected byte retries = 2;
    protected byte[] serial;
    protected long delayAfterCommand = 2000;
    protected int MIN_BUFFER_SIZE_TO_START_READING = 14;
//    protected int MAX_RESPONSE_SIZE = 64;

    CommandBase(byte[] serial) {
        this.serial = serial;
    }

    public byte getCode() {
        return CODE;
    }

    public int getMaxRecords() {
        return MAX_RECORDS;
    }

    public int getBytesPerRecord() {
        return BYTES_PER_RECORD;
    }

    public byte[] getParams() {
        return PARAMS;
    }

    public void setRetries(byte retries) {
        this.retries = retries;
    }

    public byte getRetries() {
        return retries;
    }

    public long getDelayAfterCommand() {
        return delayAfterCommand;
    }


    public int getMinBufferSizeToStartReading() {
        return MIN_BUFFER_SIZE_TO_START_READING;
    }


    public TransmitPacketRequest getTransmitPacketRequest() {
        return new TransmitPacketRequest(serial, this, retries);
    }
}