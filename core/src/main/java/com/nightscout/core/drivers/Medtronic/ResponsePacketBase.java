package com.nightscout.core.drivers.Medtronic;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ResponsePacketBase {
    protected byte[] ack;
    protected byte[] data;
    protected ByteBuffer dataBuffer;
    protected byte[] packet;

    public ResponsePacketBase() {

    }

    public ResponsePacketBase(byte[] response) {
        this.ack = Arrays.copyOfRange(response, 0, 2);
        this.data = Arrays.copyOfRange(response, 3, response.length);
        this.dataBuffer = ByteBuffer.wrap(data);
    }

    public byte[] getAck() {
        return ack;
    }
}
