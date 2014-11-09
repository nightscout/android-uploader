package com.nightscout.android.dexcom;

public enum DownloadStatus {
    SUCCESS(0),
    NO_DATA(1),
    DEVICE_NOT_FOUND(2),
    IO_ERROR(3),
    APPLICATION_ERROR(4),
    NONE(6),
    UNKNOWN(7),
    REMOTE_DISCONNECTED(8);

    private int id;

    DownloadStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
