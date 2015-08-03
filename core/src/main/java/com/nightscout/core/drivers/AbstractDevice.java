package com.nightscout.core.drivers;

import com.nightscout.core.Timestamped;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;

import net.tribe7.common.base.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a representation for a device, which contains data that we want.
 */
abstract public class AbstractDevice {
    public final Download downloadAllAfter(Optional<Timestamped> timestamped) {
        DownloadStatus status;
        // TODO(trhodeos): catch more exception types.
        try {
            return doDownloadAllAfter(timestamped);
        } catch (DeviceNotFoundException e) {
            getReporter().report(EventType.DEVICE, EventSeverity.WARN, e.getMessage());
            status = DownloadStatus.DEVICE_NOT_FOUND;
        } catch (Exception e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR, "Unknown error - " + e.getMessage());
            status = DownloadStatus.UNKNOWN;
        }
        return new Download.Builder().status(status)
            .timestamp(new DateTime().toString()).build();
    }

    public abstract boolean isConnected();

    protected abstract Download doDownloadAllAfter(Optional<Timestamped> timestamped);

    protected abstract EventReporter getReporter();
}
