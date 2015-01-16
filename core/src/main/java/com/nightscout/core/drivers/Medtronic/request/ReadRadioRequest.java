package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.drivers.Medtronic.OpCodes;
import com.nightscout.core.utils.CRC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ReadRadioRequest extends RequestBase {
    public ReadRadioRequest(short size) {
        OPCODE = OpCodes.READ_RADIO;
//        MAX_RESPONSE_SIZE = size;
        byte[] me = ByteBuffer.allocate(2).putShort(size).array();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(OPCODE);
            outputStream.write(me);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte crc = CRC.calculate8(outputStream.toByteArray());
        outputStream.write(crc);
        packet = outputStream.toByteArray();
    }
}