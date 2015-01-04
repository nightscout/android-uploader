package com.nightscout.core.drivers.Medtronic.remote_commands;


public class ReadPumpModelCommand extends CommandBase {

    public ReadPumpModelCommand(byte[] serial) {
        super(serial);
        this.CODE = (byte) 0x8D;
        this.MAX_RECORDS = 1;
        this.BYTES_PER_RECORD = 64;
        this.PARAMS = new byte[0];
        this.retries = 5;
//        this.MAX_RESPONSE_SIZE = 78;
    }

}
