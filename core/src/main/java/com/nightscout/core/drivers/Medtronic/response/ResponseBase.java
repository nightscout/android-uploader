package com.nightscout.core.drivers.Medtronic.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class ResponseBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected byte[] ack;
    protected byte[] data;
    protected ByteBuffer dataBuffer;
    protected byte[] packet;

    public ResponseBase() {

    }

    public ResponseBase(byte[] response) {
        this.ack = Arrays.copyOfRange(response, 0, 2);
        this.data = Arrays.copyOfRange(response, 3, response.length);
        this.dataBuffer = ByteBuffer.wrap(data);
    }

    public byte[] getData() {
        return data;
    }
}