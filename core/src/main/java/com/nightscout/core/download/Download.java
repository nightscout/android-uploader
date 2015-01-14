package com.nightscout.core.download;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Download object that maintains the state of a download from a device
 */
abstract public class Download {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected DateTime downloadTimestamp;
    protected DownloadStatus status = DownloadStatus.NONE;
    protected int uploaderBattery;

    public Download(DateTime downloadTimestamp, DownloadStatus status, int uploaderBattery){
        this.downloadTimestamp=checkNotNull(downloadTimestamp);
        this.status=checkNotNull(status);
        this.uploaderBattery=checkNotNull(uploaderBattery);
    }

    public DateTime getDownloadTimestamp() {
        return downloadTimestamp;
    }

    public void setDownloadTimestamp(DateTime downloadTimestamp) {
        this.downloadTimestamp = downloadTimestamp;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public int getUploaderBattery() {
        return uploaderBattery;
    }

    public void setUploaderBattery(int uploaderBattery) {
        this.uploaderBattery = uploaderBattery;
    }

    abstract public byte[] toProtobufByteArray();
}
