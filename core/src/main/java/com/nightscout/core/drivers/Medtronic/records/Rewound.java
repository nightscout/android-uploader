package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Rewound extends TimeStampedRecord {

    public Rewound(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}