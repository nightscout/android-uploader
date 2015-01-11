package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class UnabsorbedInsulin extends VariableSizeBodyRecord {
    public UnabsorbedInsulin(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = data[1];
    }
}
