package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Sara6E extends Record {
    public Sara6E(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 1;
        timestampSize = 2;
        bodySize = 49;
    }

    @Override
    protected void decode(byte[] data) {

    }
}