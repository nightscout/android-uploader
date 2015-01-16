package com.nightscout.core.drivers.Medtronic.records;


import com.nightscout.core.drivers.Medtronic.PumpModel;

public class ChangeRemoteId extends TimeStampedRecord {

    public ChangeRemoteId(byte[] data, PumpModel model) {
        super(data, model);
        this.decode(data);
    }
}