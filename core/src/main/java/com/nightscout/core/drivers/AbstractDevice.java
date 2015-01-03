package com.nightscout.core.drivers;

import com.nightscout.core.model.DownloadResults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a representation for a device that we want information from e.g. pump or cgm
 */
abstract public class AbstractDevice {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected String deviceName = "Unknown";
    protected DeviceTransport transport;

    public AbstractDevice(DeviceTransport transport) {
        this.transport = transport;
    }

    public abstract boolean isConnected();

    // Not sure that I'll need this in the general device. This may be required for only push based
    // devices.
    protected void onDownload() {
    }

    public final DownloadResults download() {
        DownloadResults download = doDownload();
        onDownload();
        return download;
    }

    public String getDeviceName() {
        return deviceName;
    }

    abstract protected DownloadResults doDownload();
}
