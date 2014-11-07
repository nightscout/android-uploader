package com.nightscout.android.analyzers;

import com.nightscout.android.dexcom.G4Download;

public abstract class AbstractDownloadAnalyzer implements DownloadAnalyzerInterface {
    protected static final String TAG = AbstractDownloadAnalyzer.class.getSimpleName();
    protected AnalyzedDownload downloadObject;

    AbstractDownloadAnalyzer(G4Download dl){
        downloadObject=new AnalyzedDownload(dl);
    }

    @Override
    public AnalyzedDownload analyze(G4Download dl) {
        downloadObject=(AnalyzedDownload) dl;
        return analyze();
    }

    public AnalyzedDownload analyze() {
        return this.downloadObject;
    }

    abstract protected void correlateMessages();

}