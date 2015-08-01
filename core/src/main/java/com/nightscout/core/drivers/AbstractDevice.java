package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.squareup.otto.Bus;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class is a representation for a cgm
 */
abstract public class AbstractDevice {
    protected final Logger log = LoggerFactory.getLogger(AbstractDevice.class);
    protected DeviceType deviceType = DeviceType.UNKNOWN;
    private Bus bus = BusProvider.getInstance();
    protected EventReporter reporter;
    protected ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",
                                                                 Locale.getDefault());
    protected G4ConnectionState connectionStatus = G4ConnectionState.CLOSED;

    public abstract boolean isConnected();

    // Not sure that I'll need this in the general device. This may be required for only push based
    // devices.
    protected void downloadComplete(Download download) {
        boolean successful = download.status == com.nightscout.core.model.v2.DownloadStatus.SUCCESS;
        log.debug("downloadComplete with {} called.", download.status.name());
        if (successful && reporter != null) {
            reporter.report(EventType.DEVICE, EventSeverity.INFO,
                            messages.getString("event_sync_log"));
        }
    }

    public final Download download() {
        try {
            Download download = doDownload();
            downloadComplete(download);
            return download;
        } catch (Exception e) {
            if (reporter != null) {
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                                "Unknown error - " + e.getMessage());
            }
            log.error("Exception: {} - {}", e.getMessage(), e);
        }
        return new Download.Builder().status(com.nightscout.core.model.v2.DownloadStatus.APPLICATION_ERROR)
            .timestamp(new DateTime().toString()).build();
    }

    abstract protected Download doDownload();

    protected void onConnect() {
        log.debug("Connection detected in abstract class");
        if (reporter != null) {
            reporter.report(EventType.DEVICE, EventSeverity.INFO,
                            messages.getString("g4_connected"));

        }
        connectionStatus = G4ConnectionState.CONNECTED;
        postConnectionStatus();
    }

    protected void onConnecting() {
        log.debug("Connecting to device");
        connectionStatus = G4ConnectionState.CONNECTING;
        postConnectionStatus();
    }

    protected void onDisconnect() {
        log.debug("Disconnection detected in abstract class");
        if (reporter != null) {
            reporter.report(EventType.DEVICE, EventSeverity.INFO,
                            messages.getString("g4_disconnected"));
        }
        connectionStatus = G4ConnectionState.CLOSED;
        postConnectionStatus();
    }

    protected void onDisconnecting() {
        log.debug("Disconnecting from device");
        connectionStatus = G4ConnectionState.CLOSING;
        postConnectionStatus();
    }

    protected void onReading() {
        log.debug("Reading from device");
        connectionStatus = G4ConnectionState.READING;
        postConnectionStatus();
    }

    protected void onWriting() {
        log.debug("Writing to device");
        connectionStatus = G4ConnectionState.WRITING;
        postConnectionStatus();
    }

    public DeviceConnectionStatus getDeviceConnectionStatus() {
        return new DeviceConnectionStatus(deviceType, connectionStatus);
    }

    private void postConnectionStatus() {
        bus.post(new DeviceConnectionStatus(deviceType, connectionStatus));
    }

    public void setReporter(EventReporter reporter) {
        this.reporter = reporter;
    }
}
