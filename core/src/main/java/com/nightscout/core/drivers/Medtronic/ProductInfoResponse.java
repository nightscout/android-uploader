package com.nightscout.core.drivers.Medtronic;

import com.nightscout.core.dexcom.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProductInfoResponse extends ResponsePacketBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private byte rfCode;
    private String serialNumber;
    private String productVersion;
    private String description;
    private String softwareVersion;
    private Map<Integer, String> interfaces;

    public ProductInfoResponse(byte[] response) {
        super(response);

        interfaces = new HashMap<>();
        serialNumber = Utils.bytesToHex(new byte[]{dataBuffer.get(0), dataBuffer.get(1), dataBuffer.get(2)});
        productVersion = String.format("%d1.%d1", dataBuffer.get(3), dataBuffer.get(4));
        rfCode = dataBuffer.get(5);
        description = new String(Arrays.copyOfRange(data, 6, 15));
        softwareVersion = String.format("%d1.%d1", dataBuffer.get(16), dataBuffer.get(17));
        interfaces = parseInterfaces();
    }

    private String getRfCode(byte rfCode) {
        switch (rfCode) {
            case 0x00:
                return "868.35Mhz";
            case 0x01:
            case (byte) 0xFF:
                return "916.5Mhz";
            default:
                return "Unknown";
        }
    }

    private Map<Integer, String> parseInterfaces() {
        byte numberOfInterfaces = dataBuffer.get(18);
        Map<Integer, String> iface = new HashMap<>();
        for (int i = 0; i < numberOfInterfaces + 1; i++) {
            if (dataBuffer.get(19 + (i + 1)) == 1) {
                iface.put((int) dataBuffer.get(19 + i), "USB");
            } else if (dataBuffer.get(19 + (i + 1)) == 3) {
                iface.put((int) dataBuffer.get(19 + i), "Paradigm RF");
            } else {
                log.info("Undefined interface");
            }
        }
        return iface;
    }

    public byte getRfCode() {
        return rfCode;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public String getDescription() {
        return description;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public Map<Integer, String> getInterfaces() {
        return interfaces;
    }
}
