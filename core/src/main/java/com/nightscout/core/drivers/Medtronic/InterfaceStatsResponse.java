package com.nightscout.core.drivers.Medtronic;

public class InterfaceStatsResponse extends ResponsePacketBase {
    private byte crcErrors;
    private byte seqErrors;
    private byte nakErrors;
    private byte timeouts;
    private Integer received;
    private Integer transmitted;

    public InterfaceStatsResponse(byte[] response) {
        super(response);
        crcErrors = dataBuffer.get(0);
        seqErrors = dataBuffer.get(1);
        nakErrors = dataBuffer.get(2);
        timeouts = dataBuffer.get(3);
        received = dataBuffer.getInt(4);
        transmitted = dataBuffer.getInt(8);
    }

    public byte getCrcErrors() {
        return crcErrors;
    }

    public byte getSeqErrors() {
        return seqErrors;
    }

    public byte getNakErrors() {
        return nakErrors;
    }

    public byte getTimeouts() {
        return timeouts;
    }

    public Integer getTransmitted() {
        return transmitted;
    }

    public Integer getReceived() {
        return received;
    }
}
