package com.nightscout.core.drivers.Medtronic.records;

import java.util.Arrays;

public class BasalProfileStart extends DatedRecord {
    public static final byte bodySize = 3;

    BasalProfileStart(byte[] data) {
        super(Arrays.copyOfRange(data, 0, 7));
    }
}
