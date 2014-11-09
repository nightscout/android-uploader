package com.nightscout.android.devices;

import com.nightscout.android.dexcom.DownloadStatus;
import com.nightscout.android.dexcom.GlucoseUnit;

public abstract class Download {
    protected GlucoseUnit unit;
    protected long downloadTimestamp;
    protected DownloadStatus downloadStatus;
    protected String driver;
    protected int deviceID;

    public abstract <T> T toCookieProtobuf();
}
