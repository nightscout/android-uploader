package com.nightscout.android.devices;

public class EGVThresholds {
    int highThreshold;
    int lowThreshold;

    EGVThresholds(int low,int high){
        highThreshold=high;
        lowThreshold=low;
    }

    public EGVThresholds(){
        highThreshold=180;
        lowThreshold=60;
    }

    public int getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(int highThreshold) {
        this.highThreshold = highThreshold;
    }

    public int getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(int lowThreshold) {
        this.lowThreshold = lowThreshold;
    }
}