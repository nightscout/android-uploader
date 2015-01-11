package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Old6c extends Record {
    public Old6c(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 38;
    }

    @Override
    protected void decode(byte[] data) {

    }
}
