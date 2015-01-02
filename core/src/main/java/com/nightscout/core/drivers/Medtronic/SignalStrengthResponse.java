package com.nightscout.core.drivers.Medtronic;

import com.google.common.primitives.UnsignedBytes;

public class SignalStrengthResponse extends ResponsePacketBase {
    private byte strength;

    public SignalStrengthResponse(byte[] response) {
        super(response);
        this.strength = dataBuffer.get();
    }

    public int getStrength() {
        return UnsignedBytes.toInt(strength);
    }
}
