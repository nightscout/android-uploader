package com.nightscout.core.dexcom;

public class InvalidRecordLengthException extends Error {
    public InvalidRecordLengthException(String message){
        super(message);
    }

    public InvalidRecordLengthException(String message,Throwable throwable){
        super(message,throwable);
    }

}
