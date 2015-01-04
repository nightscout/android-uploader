package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Sara6E extends Record {
    public Sara6E(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 2;
        bodySize = 48;
    }
}
