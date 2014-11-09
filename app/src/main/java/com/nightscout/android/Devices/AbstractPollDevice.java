package com.nightscout.android.devices;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.Nightscout;
import com.nightscout.android.dexcom.G4Download;

abstract public class AbstractPollDevice extends AbstractDevice {
    private static final String TAG = AbstractPollDevice.class.getSimpleName();
    // Set the default pollInterval to 5 minutes and 3 seconds...
    protected long pollInterval=303000;
    protected DeviceTransportAbstract transport;

    public AbstractPollDevice(int deviceID, Context context, String driver){
        super(deviceID,context,driver);
    }

    abstract protected G4Download doDownload();

    public void download(){
        Log.i(TAG,"Before download thread creation");
        Tracker tracker=((Nightscout) context.getApplicationContext()).getTracker();
        long downloadTimeStart=System.currentTimeMillis();
        G4Download dl=doDownload();
        tracker.send(new HitBuilders.TimingBuilder()
                        .setCategory("Device Download")
                        .setLabel(driver)
                        .setValue(System.currentTimeMillis() - downloadTimeStart)
                        .setVariable(driver)
                        .build()
        );
        Log.i(TAG,"Download complete in download thread");
        onDownload(dl);
    }

    public void setPollInterval(int pollInterval) {
        Log.d(TAG,"Setting poll interval to: "+pollInterval);
        this.pollInterval = pollInterval;
    }

    public long getPollInterval() {
        return pollInterval;
    }
}