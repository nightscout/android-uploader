package com.nightscout.android.dexcom;

import java.util.ArrayList;


public class DexcomCommandPacket {
    public static final int MAX_PAYLOAD = 1584;
    public static final int MIN_LEN = 6;
    public static final int MAX_LEN = MAX_PAYLOAD + MIN_LEN;
    public static final byte SOF = 0x01;
    public static final int OFFSET_SOF = 0;
    public static final int OFFSET_LENGTH = 1;
    public static final int OFFSET_CMD = 3;
    public static final int OFFSET_PAYLOAD = 4;
    private static final String TAG = DexcomCommandPacket.class.getSimpleName();
    private ArrayList<Byte> packet;
    private int command;
    private ArrayList<Byte> payload;

    public DexcomCommandPacket(int command) {
        this.command = command;
    }

    private void resetPacket() {
        packet =  new ArrayList<Byte>();
        packet.add(SOF);
        setLength();
        packet.add((byte) 0);
        packet.add((byte) command);
    }

    public static short calculateCRC16(byte[] buff, int start, int end) {

        int crc = 0;
        for (int i = start; i < end; i++) {

            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (buff[i] & 0xff);
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;

        }
        crc &= 0xffff;
        return (short) crc;

    }

    private void setLength() {
        byte packetSize = (byte) Math.max(this.packet.size() + 2, MIN_LEN);
        if (this.packet.size() < 2) {
            packet.add(packetSize);
        } else {
            packet.set(1, packetSize);
        }
    }

    private void setCRC16() {
        short crc16 = calculateCRC16(bytes(), 0, this.packet.size());
        this.packet.add((byte) (crc16 & 0xff));
        this.packet.add((byte) ((crc16 >> 8) & 0xff));
    }

    private byte[] bytes() {
        byte[] b = new byte[this.packet.size()];
        for (int i = 0; i < this.packet.size(); i++) {
            b[i] = this.packet.get(i).byteValue();
        }
        return b;
    }

    public void setPayload(ArrayList<Byte> payload) {
        this.payload = payload;
    }

    public byte[] Compose() {
        resetPacket();
        if (this.payload != null) {
            this.packet.addAll(this.payload);
        }
        setLength();
        setCRC16();
        return this.bytes();
    }

}
