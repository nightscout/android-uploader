package com.nightscout.android.dexcom;

public class CRCFailRuntimeException extends RuntimeException {
    CRCFailRuntimeException(String message){
        super(message);
    }
}
