package com.nightscout.android.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.nightscout.core.dexcom.records.GlucoseDataSet;

public class SMSSender {

    private static final String TAG = SMSSender.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);

    private static SkipCounter smsSkipCount;
    private static SkipCounter inRangeSkipCount;
    private static SkipCounter noDataSkipCount;

    private static Integer previousBg = 0;
    private static Date previousDisplayTime;
    private static String previousBgMessage = "";

    private static String bgCurrentMessage = "Awaiting first CGM reading...";

    private Context mContext;
    private static boolean ignoreSkipCounter;

    public SMSSender(Context context) {
        mContext = context;
        prepareCounters();
    }


    private void prepareCounters() {
        if(smsSkipCount == null)smsSkipCount = new SkipCounter("DATA_POINT_SMS_INTERVAL");
        if(inRangeSkipCount == null)inRangeSkipCount =  new SkipCounter("IN_RANGE_INTERVAL");
        if(noDataSkipCount == null)noDataSkipCount =  new SkipCounter("NO_DATA_INTERVAL");

        smsSkipCount.setContext(mContext);
        inRangeSkipCount.setContext(mContext);
        noDataSkipCount.setContext(mContext);
    }

    public void processForSMS(List<GlucoseDataSet> records) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Boolean enableSMS = prefs.getBoolean("SMS_ENABLE", false);

        if(!enableSMS)return;

        if(records.size() >= 20){
            processForSMS(records.get(records.size() -1));
            return;
        }

        for (GlucoseDataSet record : records) {
            processForSMS(record);
        }
    }


    private void processForSMS(GlucoseDataSet record) {
        if(isNoDataRecord(record)){
            if(noDataSkipCount.isSkipCountMet(ignoreSkipCounter))sendSMS(mContext,getNoDataMsg());
            return;
        }

        //Valid data point received, reset no data skip count;
        noDataSkipCount.reset();

        int BGInt = record.getBGValue();
        bgCurrentMessage = getBGMessage(record, BGInt);

        if (shouldSendSMS(record,BGInt)) sendSMS(mContext,bgCurrentMessage);

        //store current values
        previousBg = BGInt;
        previousDisplayTime = record.getDisplayTime();
    }


    private boolean isNoDataRecord(GlucoseDataSet record) {
        if(getMinutesDate(record.getDisplayTime()) > 10)return true;
        if(previousBg == 0 || previousDisplayTime == null)return false;

        if(record.getDisplayTime().compareTo(previousDisplayTime) == 0)return true;
        return false;
    }


    private boolean shouldSendSMS(GlucoseDataSet record,int BGInt){
        // Severe hypo. Send every data point until over 55.
        if(BGInt < 55 && BGInt > 38) return true;

        if(previousBg == 0) return true;
        //Load BG thresholds from preference
        int SMSBgHigh = getIntPreferenceFromString(mContext,"SMSBgHigh");
        int SMSBgLow = getIntPreferenceFromString(mContext,"SMSBgLow");

        // BG in range.  Do not send text. Reset skipCount to reset value.
        if(BGInt >= SMSBgLow && BGInt <= SMSBgHigh){
            smsSkipCount.reset();
            return inRangeSkipCount.isSkipCountMet(ignoreSkipCounter);
        }

        // BG is out of range, reset in-range skip count
        inRangeSkipCount.reset();



        //Return true if skip count is met
        return smsSkipCount.isSkipCountMet(ignoreSkipCounter);

    }
    private static int getIntPreferenceFromString(Context mContext, String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String stringValue = prefs.getString(key, String.valueOf(SkipCounter.RESET_VALUE));
        int value = SkipCounter.RESET_VALUE;
        try{
            value = Integer.valueOf(stringValue);
        }catch(Exception e){
        }
        return value;

    }
    private static String getStringPreference(Context mContext,String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getString(key, "");
    }

    private String getBGMessage(GlucoseDataSet record, int intBGInt) {
        String bgMessage = "";

        if(previousBg == 0){
            bgMessage = record.getBGValue() + " (first reading)";
        }
        else if(previousBg >0 && previousBg < 39) {
            bgMessage = String.valueOf(record.getBGValue());
            //bgMessage = record.getBGValue() + " (first reading)";
            //bgMessage = "Connected and awaiting first reading...";
            //}
            //else if (previousBg == -1){
            //    bgMessage = record.getBGValue() + " " + getNoDataMsg();
        }
        else if (intBGInt == previousBg){
            bgMessage = record.getBGValue() + " stable ";
        }
        else if(intBGInt > previousBg){
            bgMessage = record.getBGValue() + " up " + (intBGInt - previousBg);
        }
        else if(intBGInt < previousBg){
            bgMessage = record.getBGValue() + " down " + (previousBg - intBGInt);
        }

        // Add additional message based on BG
        if (intBGInt < 55 && intBGInt > 38){
            bgMessage = bgMessage + " - UNDER 55";
        }
        else if (intBGInt == 1){
            bgMessage = "Dexcom Error: Sensor Not Active";
        }
        else if (intBGInt == 2){
            bgMessage = "Dexcom Error: Minimal Deviation";
        }
        else if (intBGInt == 3){
            bgMessage = "Dexcom Error: No Antenna";
        }
        else if (intBGInt == 4){
            bgMessage = "Dexcom Error: Code 4 (Unspecified)";
        }
        else if (intBGInt == 5){
            bgMessage = "Dexcom Error: Sensor Not Calibrated";
        }
        else if (intBGInt == 6){
            bgMessage = "Dexcom Error: Counts Deviation";
        }
        else if (intBGInt == 7){
            bgMessage = "Dexcom Error: Code 7 (Unspecified)";
        }
        else if (intBGInt == 8){
            bgMessage = "Dexcom Error: Code 8 (Unspecified)";
        }
        else if (intBGInt == 9){
            bgMessage = "Dexcom Error: Absolute Deviation";
        }
        else if (intBGInt == 10){
            bgMessage = "Dexcom Error: Power Deviation";
        }
        else if (intBGInt == 12){
            bgMessage = "Dexcom Error: Bad RF";
        }


        String SMStime = getTimeStamp(record.getDisplayTime());
        String SMSmsg = SMStime + " - " + bgMessage;

        if(!bgMessage.contains("No current"))previousBgMessage = bgMessage;

        return SMSmsg;
    }

    private static String getNoDataMsg() {
        String msg = "No data ";
        if(previousDisplayTime!=null){
            msg+= "since " + getTimeStamp(previousDisplayTime);
        }else{
            msg+= " (initial read)";
        }
        return msg;
    }

    private static String getTimeStamp(Date displayTime) {
        if(displayTime == null)return "";
        return DATE_FORMAT.format(displayTime);
    }


    public static void sendSMSAsync(final Context mContext, final String msg){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                sendSMS(mContext,msg);
                return null;
            }}.execute();

    }
    public static void sendSMS(Context mContext,String msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Boolean enableSMS = prefs.getBoolean("SMS_ENABLE", false);
        if(!enableSMS)return;

        String receivers = getStringPreference(mContext,"SMS_RECEIVER");

        if(!receivers.contains(",")){
            sendSMSDevice(receivers, msg);
            return;
        }

        String[] senders = receivers.split(",");
        for(String receiver:senders){
            sendSMSDevice(receiver, msg);
        }

    }
    public long getMinutesDate(Date dateTime){
        try{
            Calendar now = Calendar.getInstance();
            Calendar reading = Calendar.getInstance();
            reading.setTime(dateTime);

            long diffInMs = now.getTime().getTime() - reading.getTime().getTime();
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            long diffInMin = TimeUnit.SECONDS.toMinutes(diffInSec);

            return diffInMin;
        }catch(Exception e){

        }
        return 0;
    }
    private static void sendSMSDevice(String receiver,String msg){
        if(TextUtils.isEmpty(receiver))return;

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(receiver, null, msg, null, null);
    }
    public static void reset() {
        ignoreSkipCounter = false;
        smsSkipCount.reset();
        inRangeSkipCount.reset();
        noDataSkipCount.reset();

        previousBg = 0;
        previousDisplayTime = null;
        previousBgMessage = "";

        bgCurrentMessage = "Awaiting first CGM reading...";
    }

    public static String getCurrentMessage(){
        return bgCurrentMessage;
    }
    public static String setCurrentMessage(String message){
        return message;
    }


}
