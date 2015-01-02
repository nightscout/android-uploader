package com.nightscout.core.drivers.Medtronic;

abstract public class CommandBase {
    protected byte CODE;
    protected int MAX_RECORDS;
    protected int BYTES_PER_RECORD;
    protected byte[] PARAMS;

}
