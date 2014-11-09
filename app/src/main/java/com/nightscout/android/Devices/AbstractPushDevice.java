package com.nightscout.android.devices;
import android.content.Context;

import com.nightscout.android.dexcom.G4Download;

abstract public class AbstractPushDevice extends AbstractDevice {
    public AbstractPushDevice(int deviceID, Context appContext, String driver) {
        super(deviceID, appContext, driver);
    }

    @Override
    public void onDownload(G4Download dl){
        super.onDownload(dl);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}