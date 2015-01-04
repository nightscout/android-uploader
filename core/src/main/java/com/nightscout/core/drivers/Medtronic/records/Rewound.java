package com.nightscout.core.drivers.Medtronic.records;

public class Rewound extends DatedRecord {
    public static final byte bodySize = 0;

    Rewound(byte[] data) {
        super(data);
    }
}
