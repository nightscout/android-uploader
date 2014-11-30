package com.nightscout.core.dexcom;

public enum InsertionState {
    NONE,
    REMOVED,
    EXPIRED,
    RESIDUAL_DEVIATION,
    COUNTS_DEVIATION,
    SECOND_SESSION,
    OFF_TIME_LOSS,
    STARTED,
    BAD_TRANSMITTER,
    MANUFACTURING_MODE,
    MAX_VALUE
}
