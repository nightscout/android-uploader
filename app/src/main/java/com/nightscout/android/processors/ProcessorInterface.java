package com.nightscout.android.processors;


import com.nightscout.android.dexcom.G4Download;

public interface ProcessorInterface {
    public boolean process(G4Download d);
    public void stop();
    public void start();
}
