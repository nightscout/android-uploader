package com.nightscout.core.drivers.Medtronic.records;


import com.nightscout.core.drivers.Medtronic.PumpModel;

public class Bolus extends TimeStampedRecord {
    private float programmedAmount;
    private float deliveredAmount;
    private float duration;
    private float unabsorbed = -1;
    private BolusType bolusType;


    public Bolus(byte[] data, PumpModel model) {
        super(data, model);
        if (model == PumpModel.MM523) {
            headerSize = 8;
        } else {
            headerSize = 4;
        }
        this.decode(data);
    }

    @Override
    protected void decode(byte[] data) {
        super.decode(data);
        programmedAmount = data[1] / 10.0f;
        deliveredAmount = data[2] / 10.0f;
        if (model == PumpModel.MM523) {
            programmedAmount = data[2] / 40.0f;
            deliveredAmount = data[4] / 40.0f;
            unabsorbed = data[6] / 40.0f;
            duration = data[7] * 30;
        } else {
            programmedAmount = data[1] / 10.0f;
            deliveredAmount = data[2] / 10.0f;
            duration = data[3] * 30;
        }
        bolusType = (duration > 0) ? BolusType.SQUARE : BolusType.NORMAL;
    }

    public enum BolusType {
        SQUARE,
        NORMAL
    }

    @Override
    public void logRecord() {
        log.info("{} {} Programmed amount: {} Delivered Amount: {} Duration: {} Type: {} Unabsorbed: {}", timeStamp, recordTypeName, programmedAmount, deliveredAmount, duration, bolusType.name(), unabsorbed);
    }
}