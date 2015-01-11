package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Prime extends TimeStampedRecord {
    private float amount;
    private float fixed;
    private PrimeType primeType;

    public Prime(byte[] data, PumpModel model) {
        super(data, model);
        headerSize = 5;
        this.decode(data);
    }

    @Override
    protected void decode(byte[] data) {
        super.decode(data);
        amount = data[4] / 10.0f;
        fixed = data[2] / 10.0f;
        primeType = (fixed == 0) ? PrimeType.MANUAL : PrimeType.FIXED;
    }

    private enum PrimeType {
        MANUAL,
        FIXED
    }

    @Override
    public void logRecord() {
        log.info("{} {} Amount: {} Fixed: {} Type: {}", timeStamp, recordTypeName, amount, fixed, primeType.name());
    }
}
