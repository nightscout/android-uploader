package com.nightscout.core.drivers;

import com.nightscout.core.BusProvider;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.Download;
import com.nightscout.core.model.DownloadStatus;
import com.nightscout.core.model.ReceiverState;
import com.nightscout.core.model.ReceiverStatus;
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
    protected G4ConnectionState connectionStatus = G4ConnectionState.CLOSED;
    protected ReceiverState receiverState;

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
            Message download = doDownload();
            // TODO figure out a way to not make this specific to the G4
            onDownload(((Download) download).download_status == DownloadStatus.SUCCESS);
            return download;
        } catch (Exception e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR, "Unknown error - " + e.getMessage());
            log.error("Exception: {} - {}", e.getMessage(), e);
            e.printStackTrace();
        }
        return new Download.Builder().download_status(DownloadStatus.APPLICATION_ERROR)
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
        connectionStatus = G4ConnectionState.CONNECTED;
        postConnectionStatus();
        receiverState = new ReceiverState(new DateTime().getMillis(), ReceiverStatus.RECEIVER_CONNECTED);
    }

    private void postReceiverState(ReceiverState receiverState) {
        Download.Builder downloadBuilder = new Download.Builder();
        log.error("Posting receiver State: {}", receiverState.event.name() + " @ " + new DateTime(receiverState.timestamp_ms).toString());
        bus.post(downloadBuilder.receiver_state(receiverState).download_timestamp(new DateTime().toString()).build());
    }

    protected void onConnecting() {
        log.debug("Connecting to device");
        connectionStatus = G4ConnectionState.CONNECTING;
        postConnectionStatus();
    }

    protected void onDisconnect() {
        log.debug("Disconnection detected in abstract class");
        reporter.report(EventType.DEVICE, EventSeverity.INFO,
                messages.getString("g4_disconnected"));
        connectionStatus = G4ConnectionState.CLOSED;
        postConnectionStatus();
        receiverState = new ReceiverState(new DateTime().getMillis(), ReceiverStatus.RECEIVER_DISCONNECTED);
        postReceiverState(receiverState);
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
