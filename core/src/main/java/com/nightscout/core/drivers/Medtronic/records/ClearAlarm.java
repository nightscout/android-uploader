package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ClearAlarm extends TimeStampedRecord {
    public ClearAlarm(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}