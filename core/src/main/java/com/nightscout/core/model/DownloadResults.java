package com.nightscout.core.model;

import org.json.JSONArray;

public class DownloadResults {
    private G4Download download;
    private long nextUploadTime;
    private JSONArray resultArray;
    private long displayTime;

    public DownloadResults(G4Download download, long nextUploadTime,
                           JSONArray resultArray, long displayTime) {
        this.download = download;
        this.nextUploadTime = nextUploadTime;
        this.resultArray = resultArray;
        this.displayTime = displayTime;
    }

    public void setDownload(G4Download download) {
        this.download = download;
    }

    public void setNextUploadTime(long nextUploadTime) {
        this.nextUploadTime = nextUploadTime;
    }

    public void setResultArray(JSONArray resultArray) {
        this.resultArray = resultArray;
    }

    public G4Download getDownload() {
        return download;
    }

    public JSONArray getResultArray() {
        return resultArray;
    }

    public long getDisplayTime() {
        return displayTime;
    }

    public long getNextUploadTime() {
        return nextUploadTime;
    }
}
