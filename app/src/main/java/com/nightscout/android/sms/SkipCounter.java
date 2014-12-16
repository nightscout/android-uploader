package com.nightscout.android.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SkipCounter{
	public static final int RESET_VALUE = 0;
	private int skipCount;
	private Context mContext;
	private String mCounterName;

	public SkipCounter(String counterName){
		mCounterName = counterName; 
		reset();
	}
	public void reset(){
		skipCount = RESET_VALUE;
	}
	public void forceMeetCounter(int skipInterval){
		skipCount = skipInterval;
	}
	public boolean isSkipCountMet(boolean shouldForceCounter) {
        int interval = getInterval();
        //Intervals <= 0 will be ignored
        if(interval <=0)return false;

        skipCount++;
		boolean skipCountMet = (skipCount >= getInterval());
		if(shouldForceCounter)skipCountMet = true;
		if(skipCountMet)skipCount = RESET_VALUE;
		return skipCountMet;
	}
	public void setContext(Context context) {
		mContext =context;
	}
    private int getInterval(){
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String stringValue = prefs.getString(mCounterName, String.valueOf(SkipCounter.RESET_VALUE));
        int value = SkipCounter.RESET_VALUE;
        try{
       	 value = Integer.valueOf(stringValue);
        }catch(Exception e){
        }
        return value;
        
   }
}