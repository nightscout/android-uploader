package com.nightscout.core.utils;

import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;

import net.tribe7.common.base.Optional;

public final class IsigReading {
    private IsigReading() {}

    private static final Double EPSILON = 0.01;

    private static double calculateEstimatedGlucoseValue(long rawValue, Calibration calibration) {
        return calibration.scale * (rawValue - calibration.intercept) / calibration.slope;
    }

    public static Optional<GlucoseReading> calculate(Optional<SensorGlucoseValue> glucoseValue,
                                                     Optional<Calibration> calibration,
                                                     Optional<RawSensorReading> rawSensorReading) {
        if (!glucoseValue.isPresent() || !calibration.isPresent() || !rawSensorReading.isPresent() ||
            Math.abs(calibration.get().slope) < EPSILON || rawSensorReading.get().unfiltered == 0) {
            return Optional.absent();
        }

        double estimatedUnfilteredBasedValue = calculateEstimatedGlucoseValue(
            rawSensorReading.get().unfiltered, calibration.get());
        double estimatedFilteredBasedValue = 1;
        if (rawSensorReading.get().filtered != 0) {
            estimatedFilteredBasedValue = calculateEstimatedGlucoseValue(rawSensorReading.get().filtered, calibration.get());
        }
        return Optional.of(GlucoseReading.mgdl(
            glucoseValue.get().glucose_mgdl * estimatedUnfilteredBasedValue
            / estimatedFilteredBasedValue));
    }
}
