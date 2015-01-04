package com.nightscout.core.drivers.Medtronic.response;


public class LinkStatusResponse extends ResponseBase {
    private short size;

    public LinkStatusResponse(byte[] response) {
        super(response);
        this.size = dataBuffer.getShort(3);
    }

    public short getSize() {
        return size;
    }
}
