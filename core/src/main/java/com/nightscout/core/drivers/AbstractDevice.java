package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.DownloadStatus;
import com.nightscout.core.model.G4Download;
import com.squareup.otto.Bus;
import com.squareup.wire.Message;

import org.joda.time.DateTime;
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
    protected ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
            Locale.getDefault());
    protected DeviceState connectionStatus = DeviceState.DISCONNECTED;

    public abstract boolean isConnected();

    // Not sure that I'll need this in the general device. This may be required for only push based
    // devices.
    protected void onDownload(boolean successful) {
        if (successful) {
            reporter.report(EventType.DEVICE, EventSeverity.INFO,
                    messages.getString("event_sync_log"));
        }
    }

    public final Message download() {
        try {
            onActivity(true);
            Message download = doDownload();
            // TODO figure out a way to not make this specific to the G4
            onDownload(((G4Download) download).download_status == DownloadStatus.SUCCESS);
            onActivity(false);
            return download;
        } catch (Exception e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "Unknown error - " + e.getMessage());
            log.error("Exception: {} - {}", e.getMessage(), e);
        }
        return new G4Download.Builder().download_status(DownloadStatus.APPLICATION_ERROR)
                .download_timestamp(new DateTime().toString()).build();
    }

    public String getDeviceName() {
        return deviceName;
    }

    abstract protected Message doDownload();

    protected void onConnect() {
        log.debug("Connection detected in abstract class");
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("g4_connected"));
        connectionStatus = DeviceState.CONNECTED;
        postConnectionStatus();
    }

    protected void onDisconnect() {
        log.debug("Disconnection detected in abstract class");
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("g4_disconnected"));
        connectionStatus = DeviceState.DISCONNECTED;
        postConnectionStatus();
    }

    public void onActivity(boolean enabled) {
        log.debug("Activity change detected for device: {}", enabled);
        if (enabled) {
            connectionStatus = DeviceState.ACTIVE;
        } else {
            connectionStatus = (isConnected()) ? DeviceState.CONNECTED : DeviceState.DISCONNECTED;
        }
        postConnectionStatus();
    }

    public DeviceConnectionStatus getDeviceConnectionStatus() {
        log.warn("Device type from device: {}", deviceType);
        return new DeviceConnectionStatus(deviceType, connectionStatus);
    }

    private void postConnectionStatus() {
        bus.post(new DeviceConnectionStatus(deviceType, connectionStatus));
    }


    public void setReporter(EventReporter reporter) {
        this.reporter = reporter;
    }
}
