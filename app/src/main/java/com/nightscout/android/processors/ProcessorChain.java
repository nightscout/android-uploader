package com.nightscout.android.processors;

import android.util.Log;

import com.nightscout.android.dexcom.G4Download;

import java.util.ArrayList;
import java.util.List;

public class ProcessorChain {
    private static final String TAG = ProcessorChain.class.getSimpleName();
    private List<AbstractProcessor> chain;

    public ProcessorChain(){
        this.chain=new ArrayList<AbstractProcessor>();
    }

    public void add(AbstractProcessor processor){
        chain.add(processor);
    }

    public void remove(AbstractProcessor processor){
        chain.remove(processor);
    }

    // TODO: Not sure this should remain named G4Download for future releases that may support other
    // systems.
    // TODO: failures in the chain need to be propagated up cleanly. Just returning a false one one
    // link fails isn't clear enough.
    public boolean process(G4Download download){
        Log.d(TAG, "XXX3: Driver: "+download.getDriver());
        boolean result = true;
        for (AbstractProcessor link:chain){
            result = result && link.process(download);
        }
        return result;
    }

    public void start(){
        for (AbstractProcessor link: chain){
            link.start();
        }
    }

    public void stop(){
        for (AbstractProcessor link: chain){
            link.stop();
        }
    }
}
