package com.nightscout.core.dexcom;

import com.google.common.base.Optional;

import java.util.Arrays;

public class ReadPacket {
    private Command command;
    private byte[] data;
    private byte[] crc;
    private int OFFSET_CMD = 3;
    private int OFFSET_DATA = 4;
    private int CRC_LEN = 2;

    public ReadPacket(byte[] readPacket) {
        Optional<Command> optCmd = Command.getCommandByValue(readPacket[OFFSET_CMD]);
        if (optCmd.isPresent()) {
            this.command = optCmd.get();
        } else {
            throw new IllegalArgumentException("Unknown command: " + readPacket[OFFSET_CMD]);
        }
        this.data = Arrays.copyOfRange(readPacket, OFFSET_DATA, readPacket.length - CRC_LEN);
        this.crc = Arrays.copyOfRange(readPacket, readPacket.length - CRC_LEN, readPacket.length);
        byte[] crc_calc = CRC16.calculate(readPacket, 0, readPacket.length - 2);
        if (!Arrays.equals(this.crc, crc_calc)) {
            throw new CRCFailError("CRC check failed. Was: " + Utils.bytesToHex(this.crc) + " Expected: " + Utils.bytesToHex(crc_calc));
        }
    }

    public Command getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
