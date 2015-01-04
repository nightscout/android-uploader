package com.nightscout.core.drivers.Medtronic.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RequestBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected byte[] packet;
    protected byte[] OPCODE;
//    protected int MAX_RESPONSE_SIZE = 64;

    public byte[] getPacket() {
        return this.packet;
    }

//    public int getMaxResponseSize() {
//        return MAX_RESPONSE_SIZE;
//    }
}
