package com.nightscout.core.dexcom;

public class CRCFailRuntimeException extends RuntimeException {
    public CRCFailRuntimeException(String message) {
        super(message);
    }
}
