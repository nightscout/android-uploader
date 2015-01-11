package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class NoDeliveryAlarm extends TimeStampedRecord {
    public NoDeliveryAlarm(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 4;
        this.decode(data);
    }
}
