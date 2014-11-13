package com.nightscout.android.dexcom;

public class CRCFailRuntimeException extends RuntimeException {
    public CRCFailRuntimeException(String message){
        super(message);
    }
}
