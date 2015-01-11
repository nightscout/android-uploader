package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class VariableSizeBodyRecord extends Record {

    public VariableSizeBodyRecord(byte[] data, PumpModel model) {
        super(data, model);
    }

    @Override
    protected void decode(byte[] data) {

    }

}
