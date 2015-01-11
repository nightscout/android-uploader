package com.nightscout.core.drivers.Medtronic.records;


import com.nightscout.core.drivers.Medtronic.PumpModel;

public class BolusWizardChange extends TimeStampedRecord {

    public BolusWizardChange(byte[] data, PumpModel model) {
        super(data, model);
        if (model == PumpModel.MM508 || model == PumpModel.MM515) {
            bodySize = 117;
        } else {
            bodySize = 143;
        }
        this.decode(data);
    }
}
