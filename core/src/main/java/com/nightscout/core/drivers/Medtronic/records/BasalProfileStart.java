package com.nightscout.core.drivers.Medtronic.records;

import com.nightscout.core.drivers.Medtronic.PumpModel;

public class BasalProfileStart extends TimeStampedRecord {
    private int offset;
    private float rate;
    private int index;

    public BasalProfileStart(byte[] data, PumpModel model) {
        super(data, model);
        bodySize = 3;
        this.decode(data);
    }

    @Override
    protected void decode(byte[] data) {
        super.decode(data);
        int bodyOffset = headerSize + timestampSize;
        offset = data[bodyOffset] * 1000 * 30 * 60;
        if (model == PumpModel.MM523) {
            rate = data[bodyOffset + 1] * 0.025f;
        }
        index = data[bodyOffset + 2];
    }

    @Override
    public void logRecord() {
        log.info("{} {} Offset: {} Rate: {}", timeStamp.toString(), recordTypeName, offset, rate);
    }
}
