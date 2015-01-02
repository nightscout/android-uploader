package com.nightscout.core.drivers.Medtronic;

public class OpCodes {
    public final static byte[] PRODUCT_INFO = new byte[]{0x04, 0x00};
    public final static byte[] RADIO_STATS = new byte[]{0x05, 0x00};
    public final static byte[] USB_STATS = new byte[]{0x05, 0x01};
    public final static byte[] SIGNAL_STRENGTH = new byte[]{0x06, 0x00};
    public final static byte[] LINK_STATUS = new byte[]{0x03, 0x00};
    public final static byte[] READ_RADIO = new byte[]{0x0C, 0x00};
    public final static byte[] TRANSMIT_PACKET = new byte[]{0x01, 0x00, (byte) 0xA7, 0x01};
}
