package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class EndResultsTotals extends Record {

    public EndResultsTotals(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 5;
        timestampSize = 2;
        bodySize = 3;
        this.decode(data);
    }

    @Override
    protected void decode(byte[] data) {

    }
}