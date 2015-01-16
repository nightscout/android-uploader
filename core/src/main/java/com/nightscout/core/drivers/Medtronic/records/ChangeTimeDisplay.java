package com.nightscout.core.drivers.Medtronic.records;


import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ChangeTimeDisplay extends TimeStampedRecord {

    public ChangeTimeDisplay(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}