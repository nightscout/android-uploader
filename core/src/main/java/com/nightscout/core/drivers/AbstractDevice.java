package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.squareup.otto.Bus;
import com.squareup.wire.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class is a representation for a device that we want information from e.g. pump or cgm
 */
abstract public class AbstractDevice {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected String deviceName = "Unknown";
    protected SupportedDevices deviceType = SupportedDevices.UNKNOWN;
    private Bus bus = BusProvider.getInstance();
    protected EventReporter reporter;
    private ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
            Locale.getDefault());

    public abstract boolean isConnected();

    // Not sure that I'll need this in the general device. This may be required for only push based
    // devices.
    protected void onDownload() {
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("event_sync_log"));
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
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("g4_connected"));
        bus.post(new DeviceConnectionStatus(deviceType, DeviceState.CONNECTED));
    }

    public void onDisconnect() {
        log.debug("Disconnection detected in abstract class");
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("g4_disconnected"));
        bus.post(new DeviceConnectionStatus(deviceType, DeviceState.DISCONNECTED));
    }

    public void onActivity(boolean enabled) {
        log.debug("Activity change detected for device: {}", enabled);
        bus.post(new DeviceConnectionStatus(deviceType, DeviceState.ACTIVE));
    }

    public class DeviceConnectionStatus {
        public SupportedDevices deviceType;
        public DeviceState deviceState;

        DeviceConnectionStatus(SupportedDevices deviceType, DeviceState deviceState) {
            this.deviceType = deviceType;
            this.deviceState = deviceState;
        }
    }

    public enum DeviceState {
        CONNECTED,
        DISCONNECTED,
        ACTIVE
    }

    public void setReporter(EventReporter reporter) {
        this.reporter = reporter;
    }
}
