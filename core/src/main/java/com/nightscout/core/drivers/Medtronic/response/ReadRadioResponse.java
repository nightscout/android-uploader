package com.nightscout.core.drivers.Medtronic.response;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.utils.CRC8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadRadioResponse extends ResponseBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean eod;
    private short resultLength;
    private byte[] data;


    public ReadRadioResponse(byte[] response) {
        this.eod = (response[5] & 0x80) > 0;
        log.info("EOD: {}", eod);
        log.info("Before modification: {}", Utils.bytesToHex(new byte[]{response[5], response[6]}));

        response[5] = (byte) (response[5] & 0x7F);
        log.info("After modification: {}", Utils.bytesToHex(new byte[]{response[5], response[6]}));
        ByteBuffer responseBuffer = ByteBuffer.wrap(response);
        this.resultLength = responseBuffer.getShort(5);
        log.info("Result length: {}", resultLength);
        log.info("Response length: {}", response.length);
        byte crc = response[response.length - 1];
        if (response.length < resultLength) {
            this.data = Arrays.copyOfRange(response, 13, 13 + resultLength);
        } else {
            this.data = Arrays.copyOfRange(response, 13, 13 + resultLength);
            log.warn("Something is wrong. Response packet is smaller than anticipated");
        }
        byte expected_crc = CRC8.calculate(this.data);
        if (crc != expected_crc) {
            log.warn("CRC Expected: {}, Was: {}", crc, expected_crc);
//            throw new CRCFailError("CRC failed");
        } else {
            log.info("CRC checks out!");
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
