package com.nightscout.android.analyzers;

import com.nightscout.android.dexcom.G4Download;

public interface DownloadAnalyzerInterface {
    public AnalyzedDownload analyze(G4Download dl);
    public AnalyzedDownload analyze();
}