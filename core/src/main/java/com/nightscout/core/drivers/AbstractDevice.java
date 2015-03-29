package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.squareup.otto.Bus;
import com.squareup.wire.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a representation for a device that we want information from e.g. pump or cgm
 */
abstract public class AbstractDevice {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected String deviceName = "Unknown";
    protected SupportedDevices deviceType = SupportedDevices.UNKNOWN;
    private Bus bus = BusProvider.getInstance();
//    protected DeviceTransport transport;

//    public AbstractDevice(DeviceTransport transport) {
//        this.transport = transport;
//    }

    public abstract boolean isConnected();

    // Not sure that I'll need this in the general device. This may be required for only push based
    // devices.
    protected void onDownload() {
    }

    public final Message download() {
        onActivity(true);
        Message download = doDownload();
        onDownload();
        onActivity(false);
        return download;
    }

    public String getDeviceName() {
        return deviceName;
    }

    abstract protected Message doDownload();

    public void onConnect() {
        log.debug("Connection detected in abstract class");
        bus.post(new DeviceConnectionStatus(deviceType, true, false));
    }

    public void onDisconnect() {
        log.debug("Disconnection detected in abstract class");
        bus.post(new DeviceConnectionStatus(deviceType, false, false));
    }

    public void onActivity(boolean enabled) {
        log.debug("Activity change detected for device: {}", enabled);
        bus.post(new DeviceConnectionStatus(deviceType, isConnected(), enabled));
    }

    public class DeviceConnectionStatus {
        public SupportedDevices deviceType;
        public boolean connected;
        public boolean active;

        DeviceConnectionStatus(SupportedDevices deviceType, boolean connected, boolean active) {
            this.deviceType = deviceType;
            this.connected = connected;
            this.active = active;
        }
    }
}
