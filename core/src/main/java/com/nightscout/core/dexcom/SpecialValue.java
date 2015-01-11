package com.nightscout.core.dexcom;

import com.google.common.base.Optional;
import com.nightscout.core.utils.GlucoseReading;

public enum SpecialValue {
    NONE("??0", 0),
    SENSOR_NOT_ACTIVE("?SN", 1),
    MINIMALLY_EGV_AB("??2", 2),
    NO_ANTENNA("?NA", 3),
    SENSOR_OUT_OF_CAL("?NC", 5),
    COUNTS_AB("?CD", 6),
    ABSOLUTE_AB("?AD", 9),
    POWER_AB("???", 10),
    RF_BAD_STATUS("?RF", 12);


    private String name;
    private int val;

    SpecialValue(String s, int i) {
        name = s;
        val = i;
    }

    public int getValue() {
        return val;
    }

    public String toString() {
        return name;
    }

    public static Optional<SpecialValue> getEGVSpecialValue(int val) {
        for (SpecialValue e : values()) {
            if (e.getValue() == val)
                return Optional.of(e);
        }
        return Optional.absent();
    }

    public static Optional<SpecialValue> getEGVSpecialValue(GlucoseReading reading) {
        return getEGVSpecialValue(reading.asMgdl());
    }

    public static boolean isSpecialValue(GlucoseReading reading) {
        return isSpecialValue(reading.asMgdl());
    }

    public static boolean isSpecialValue(int val) {
        return getEGVSpecialValue(val).isPresent();
    }

}
