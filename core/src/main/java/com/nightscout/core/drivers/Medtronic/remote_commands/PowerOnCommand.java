package com.nightscout.core.drivers.Medtronic.remote_commands;

public class PowerOnCommand extends CommandBase {
    public PowerOnCommand(byte[] serial) {
        super(serial);
        this.CODE = 93;
        this.MAX_RECORDS = 0;
        this.BYTES_PER_RECORD = 64;
        // 0x0A = stay powered on for 10 minutes
        this.PARAMS = new byte[]{0x01, 0x0A};
        this.delayAfterCommand = 17000;
//        this.delayAfterCommand = 30;
        this.retries = 0;
    }
}
