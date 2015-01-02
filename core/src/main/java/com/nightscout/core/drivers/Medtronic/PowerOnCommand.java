package com.nightscout.core.drivers.Medtronic;

public class PowerOnCommand extends CommandBase {
    public PowerOnCommand() {
        this.CODE = 93;
        this.MAX_RECORDS = 0;
        this.BYTES_PER_RECORD = 64;
        // 0x0A = 10 minutes
        this.PARAMS = new byte[]{0x01, 0x0A};
    }
}
