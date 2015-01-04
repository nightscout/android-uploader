package com.nightscout.core.drivers.Medtronic.records;

public class LowBattery extends DatedRecord {
    LowBattery(byte[] data) {
        super(data);
    }
}
