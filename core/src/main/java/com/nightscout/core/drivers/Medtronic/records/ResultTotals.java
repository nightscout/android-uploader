package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ResultTotals extends Record {
    public ResultTotals(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 40;
        headerSize = 3;
    }

    @Override
    protected void decode(byte[] data) {

    }
}