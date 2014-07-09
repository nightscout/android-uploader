package com.nightscout.android.dexcom;

import java.io.Serializable;

public class EGVRecord implements Serializable {
    public String displayTime = "---";
    public String bGValue = "---";
    public String trend ="---";
    public String trendArrow = "---";
    
    private static final long serialVersionUID = 4654897646L;
    
    public void setDisplayTime (String input) {
    	this.displayTime = input;
    }
    
    public void setBGValue (String input) {
    	this.bGValue = input;
    }
    
    public void setTrend (String input) {
    	this.trend = input;
    }
    
    public void setTrendArrow (String input) {
    	this.trendArrow = input;
    }
}

