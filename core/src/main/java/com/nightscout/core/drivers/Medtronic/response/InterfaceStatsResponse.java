package com.nightscout.core.drivers.Medtronic.response;

/**
 * This class should be used in response to both UsbInterfaceStatsRequest and
 * RadioInterfaceStatsRequest
 */
public class InterfaceStatsResponse extends ResponseBase {
    private byte crcErrors;
    private byte seqErrors;
    private byte nakErrors;
    private byte timeouts;
    private Integer received;
    private Integer transmitted;

    public InterfaceStatsResponse(byte[] response) {
        super(response);
        log.info("Response length: {}", response.length);
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