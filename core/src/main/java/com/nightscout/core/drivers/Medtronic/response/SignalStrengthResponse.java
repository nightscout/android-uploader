package com.nightscout.core.drivers.Medtronic.response;

import com.google.common.primitives.UnsignedBytes;

public class SignalStrengthResponse extends ResponseBase {
    private byte strength;

    public SignalStrengthResponse(byte[] response) {
        super(response);
        this.strength = dataBuffer.get();
    }

    public int getStrength() {
        return UnsignedBytes.toInt(strength);
    }
}
