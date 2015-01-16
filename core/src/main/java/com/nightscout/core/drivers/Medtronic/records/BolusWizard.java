package com.nightscout.core.drivers.Medtronic.records;


import com.google.common.primitives.UnsignedBytes;
import com.nightscout.core.drivers.Medtronic.PumpModel;

public class BolusWizard extends TimeStampedRecord {
    private float correction;
    private long bg;
    private int carbInput;
    private float carbRatio;
    private int sensitivity;
    private int bgTargetLow;
    private int bgTargetHigh;
    private float bolusEstimate;
    private float foodEstimate;
    private float unabsorbedInsulinTotal;

    public BolusWizard(byte[] data, PumpModel model) {
        super(data, model);
        if (model.ordinal() < PumpModel.MM523.ordinal()) {
            bodySize = 13;
        } else if (model.ordinal() >= PumpModel.MM523.ordinal()) {
            bodySize = 15;
        }
        this.decode(data);
    }

    @Override
    protected void decode(byte[] data) {
        super.decode(data);
        int bodyIndex = headerSize + timestampSize;
        bg = (((data[bodyIndex + 1] & 0x0F) << 8) | UnsignedBytes.toInt(data[1]));
        carbInput = data[bodyIndex];
        if (model == PumpModel.MM523) {
            correction = UnsignedBytes.toInt(data[bodyIndex]) / 40.0f;
            carbRatio = UnsignedBytes.toInt(data[bodyIndex + 14]) / 10.0f;
            sensitivity = UnsignedBytes.toInt(data[bodyIndex + 4]);
            bgTargetLow = data[bodyIndex + 5];
            bgTargetHigh = data[bodyIndex + 3];
            bolusEstimate = data[bodyIndex + 13] / 40.0f;
            foodEstimate = data[bodyIndex + 8] / 40.0f;
            unabsorbedInsulinTotal = data[bodyIndex + 11] / 40.0f;
        } else {
            correction = (UnsignedBytes.toInt(data[bodyIndex + 7]) + data[bodyIndex + 5] & 0x0F) / 10.0f;
            carbRatio = UnsignedBytes.toInt(data[bodyIndex + 2]);
            sensitivity = UnsignedBytes.toInt(data[bodyIndex + 3]);
            bgTargetLow = data[bodyIndex + 4];
            bgTargetHigh = data[bodyIndex + 12];
            bolusEstimate = data[bodyIndex + 11] / 10.0f;
            foodEstimate = data[bodyIndex + 6] / 10.0f;
            unabsorbedInsulinTotal = data[bodyIndex + 9] / 10.0f;
        }

    }

    @Override
    public void logRecord() {
        log.info("Time: {} RecordType: {} Bg: {} Carb Input: {} Correction: {} Carb Ratio: {} Sensitivity: {} BG Target High: {} BG Target Low: {} Bolus Estimate: {} Food Estimate: {} Unabsorbed Insulin Total: {}", timeStamp.toString(), recordTypeName, bg, carbInput, correction, carbRatio, sensitivity, bgTargetHigh, bgTargetLow, bolusEstimate, foodEstimate, unabsorbedInsulinTotal);
    }
}