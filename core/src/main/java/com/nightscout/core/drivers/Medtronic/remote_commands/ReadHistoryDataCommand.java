package com.nightscout.core.drivers.Medtronic.remote_commands;

public class ReadHistoryDataCommand extends CommandBase {
    public ReadHistoryDataCommand(byte[] serial, byte pageNum) {
        super(serial);
        this.CODE = (byte) 0x80;
        this.MAX_RECORDS = 16;
        this.BYTES_PER_RECORD = 64;
        // 0x0A = stay powered on for 10 minutes
        this.PARAMS = new byte[]{pageNum};
        this.delayAfterCommand = 2000;
        this.MIN_BUFFER_SIZE_TO_START_READING = 200;
    }
}