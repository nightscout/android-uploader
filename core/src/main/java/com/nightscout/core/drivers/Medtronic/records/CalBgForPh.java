package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class CalBgForPh extends TimeStampedRecord {
    public CalBgForPh(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}