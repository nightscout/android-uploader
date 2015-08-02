package com.nightscout.core.drivers;

import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class is a representation for a device, which contains data that we want.
 */
abstract public class AbstractDevice {
    protected final Logger log = LoggerFactory.getLogger(AbstractDevice.class);

    public final Download download() {
        try {
            return doDownload();
        } catch (Exception e) {
            getReporter().report(EventType.DEVICE, EventSeverity.ERROR, "Unknown error - " + e.getMessage());
        }
        return new Download.Builder().status(DownloadStatus.APPLICATION_ERROR)
            .timestamp(new DateTime().toString()).build();
    }

    public abstract boolean isConnected();

    protected abstract Download doDownload();

    protected abstract EventReporter getReporter();
}
