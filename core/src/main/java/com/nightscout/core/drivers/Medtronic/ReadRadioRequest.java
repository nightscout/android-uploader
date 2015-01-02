package com.nightscout.core.drivers.Medtronic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// Placholder - just using opCode directly for now
public class ReadRadioRequest {
    private byte[] packet;

    public ReadRadioRequest(short size) {
        byte[] opCode = OpCodes.READ_RADIO;
        byte[] me = ByteBuffer.allocate(2).putShort(size).array();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(opCode);
            outputStream.write(me);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte crc = CRC8.calculate(outputStream.toByteArray());
        outputStream.write(crc);
        packet = outputStream.toByteArray();
    }

    public byte[] getPacket() {
        return packet;
    }
}
