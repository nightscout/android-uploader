package com.nightscout.core.dexcom;

public class CRCFailError extends Error {
    public CRCFailError(String message) {
        super(message);
    }
}
