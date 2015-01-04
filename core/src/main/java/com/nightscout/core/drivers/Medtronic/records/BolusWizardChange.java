package com.nightscout.core.drivers.Medtronic.records;


public class BolusWizardChange extends DatedRecord {
    public static final byte bodySize = 0;

    BolusWizardChange(byte[] data, String model) {
        super(data);
    }
}
