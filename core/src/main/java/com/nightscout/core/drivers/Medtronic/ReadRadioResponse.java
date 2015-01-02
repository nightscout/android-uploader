package com.nightscout.core.drivers.Medtronic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadRadioResponse {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean eod;
    private short resultLength;
    private byte[] data;


    public ReadRadioResponse(byte[] response) {
        this.eod = (response[5] & 0x80) > 0;
        log.info("EOD: {}", eod);
        response[5] = (byte) (response[5] & 0x7F);
        ByteBuffer responseBuffer = ByteBuffer.wrap(response);
        this.resultLength = responseBuffer.getShort(5);
        log.info("Result length: {}", resultLength);
        byte crc = response[response.length - 1];
        if (response.length < 13 + resultLength) {
            this.data = Arrays.copyOfRange(response, 13, 13 + resultLength);
        } else {
            log.info("Something is wrong");
        }
        byte[] crc_data = Arrays.copyOfRange(response, 0, 13 + resultLength);
        byte expected_crc = CRC8.calculate(crc_data);
        if (crc != expected_crc) {
            log.warn("CRC Expected: {}, Was: {}", crc, expected_crc);
//            throw new CRCFailError("CRC failed");
        }
    }

    public short getResultLength() {
        return resultLength;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isEOD() {
        return eod;
    }
}
