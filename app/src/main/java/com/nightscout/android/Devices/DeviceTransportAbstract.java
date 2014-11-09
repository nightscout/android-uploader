package com.nightscout.android.devices;

import java.io.IOException;

abstract public class DeviceTransportAbstract {
    protected boolean isopen=false;
    protected int totalBytesRead=0;
    protected int totalBytesWritten=0;
    protected static final int DEFAULT_READ_TIMEOUT=1000;
    protected static final int DEFAULT_WRITE_TIMEOUT=1000;

    abstract public boolean open() throws IOException;

    abstract public void close() throws IOException;

    abstract public int read(byte[] responseBuffer,int timeoutMillis) throws IOException;

    abstract public int write(byte[] packet,int timeoutMillis) throws IOException;

    public int read(byte[] responseBuffer) throws IOException {
        return read(responseBuffer,DEFAULT_READ_TIMEOUT);
    }

    public int write(byte[] packet) throws IOException {
        return write(packet,DEFAULT_WRITE_TIMEOUT);
    }

    public boolean isOpen(){
        return isopen;
    }
}