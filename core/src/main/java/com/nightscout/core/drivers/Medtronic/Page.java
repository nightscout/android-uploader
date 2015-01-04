package com.nightscout.core.drivers.Medtronic;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.utils.CRC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Page {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private byte[] crc;
    private byte[] data;

    public Page(byte[] rawPage) {
        if (rawPage.length != 1024) {
            throw new IllegalArgumentException("Unexpected page size. Expected: 1024 Was: " + rawPage.length);
        }
        log.info("Parsing page");
        this.data = Arrays.copyOfRange(rawPage, 0, 1022);
        this.crc = Arrays.copyOfRange(rawPage, 1022, 1024);
        byte[] expectedCrc = CRC.calculate16CCITT(this.data);
        log.info("Data length: {}", data.length);
        if (!Arrays.equals(crc, expectedCrc)) {
            log.warn("CRC does not match expected value. Expected: {} Was: {}", Utils.bytesToHex(expectedCrc), Utils.bytesToHex(crc));
        } else {
            log.info("CRC checks out");
        }
    }
}
