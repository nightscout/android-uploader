package com.nightscout.android.Devices;

import com.nightscout.android.dexcom.G4Download;

public interface DeviceInterface {
    public void start();
    public void stop();
    public void fireMonitors(G4Download dl);
    public void stopMonitors();
}