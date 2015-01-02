package com.nightscout.core.drivers.Medtronic;


public class LinkStatusResponse extends ResponsePacketBase {
    private short size;

    public LinkStatusResponse(byte[] response) {
        super(response);
        this.size = dataBuffer.getShort(3);
    }

    public short getSize() {
        return size;
    }
}
