package com.nightscout.android.devices;

public class EGVLimits {
    EGVThresholds criticalThreshold=new EGVThresholds(60,300);
    EGVThresholds warnThreshold=new EGVThresholds(70,180);

    public EGVThresholds getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(EGVThresholds criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public void setCriticalThreshold(int low, int high) {
        this.criticalThreshold=new EGVThresholds(low,high);
    }

    public EGVThresholds getWarnThreshold() {
        return warnThreshold;
    }

    public void setWarnThreshold(EGVThresholds warnThresholds) {
        this.warnThreshold = warnThresholds;
    }

    public void setWarnThreshold(int low, int high) {
        this.warnThreshold=new EGVThresholds(low,high);
    }

    public int getCriticalHigh(){
        return this.criticalThreshold.getHighThreshold();
    }

    public int getCriticalLow(){
        return this.criticalThreshold.getLowThreshold();
    }

    public int getWarnHigh(){
        return this.warnThreshold.getHighThreshold();
    }

    public int getWarnLow(){
        return this.warnThreshold.getLowThreshold();
    }

    public void setCriticalHigh(int high){
        this.criticalThreshold.setHighThreshold(high);
    }

    public void setCriticalLow(int low){
        this.criticalThreshold.setLowThreshold(low);
    }

    public void setWarnHigh(int high){
        this.warnThreshold.setHighThreshold(high);
    }

    public void setWarnLow(int low){
        this.warnThreshold.setLowThreshold(low);
    }

}