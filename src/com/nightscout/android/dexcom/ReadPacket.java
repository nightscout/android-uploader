package com.nightscout.android.dexcom;

import java.util.Arrays;

public class ReadPacket {
    private int command;
    private byte[] data;
    private int OFFSET_CMD = 3;
    private int OFFSET_DATA = 4;
    private int CRC_LEN = 2;

    public ReadPacket(byte[] readPacket) {
        this.command = readPacket[OFFSET_CMD];
        this.data = Arrays.copyOfRange(readPacket, OFFSET_DATA, readPacket.length - CRC_LEN);
    }

    public int getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
