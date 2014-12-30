package com.nightscout.core.model;

import org.json.JSONArray;

public class DownloadResults {
    private CookieMonsterDownload download;
    private long nextUploadTime;
    private JSONArray resultArray;
    private long displayTime;

    public DownloadResults(CookieMonsterDownload download, long nextUploadTime,
                           JSONArray resultArray, long displayTime) {
        this.download = download;
        this.nextUploadTime = nextUploadTime;
        this.resultArray = resultArray;
        this.displayTime = displayTime;
    }

    public void setDownload(CookieMonsterDownload download) {
        this.download = download;
    }

    public void setNextUploadTime(long nextUploadTime) {
        this.nextUploadTime = nextUploadTime;
    }

    public void setResultArray(JSONArray resultArray) {
        this.resultArray = resultArray;
    }

    public CookieMonsterDownload getDownload() {
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
