package com.nightscout.core.drivers.Medtronic;


public class ReadPumpModelCommand extends CommandBase {

    public ReadPumpModelCommand() {
        this.CODE = (byte) 0x8D;
        this.MAX_RECORDS = 1;
        this.BYTES_PER_RECORD = 64;
        this.PARAMS = new byte[0];
    }

}
