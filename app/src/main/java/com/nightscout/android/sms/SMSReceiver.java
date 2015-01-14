package com.nightscout.android.sms;

import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {
	private String TAG = getClass().getSimpleName();
	private SharedPreferences prefs;

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null)return;
		
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean enableSMSReply = prefs.getBoolean("SMS_REPLY", false);
        if (!enableSMSReply)return;
        
		List<SMSMessage> smsLoads = new ArrayList<SMSMessage>();
		smsLoads = extractSMSLoads(bundle);
		processSMSLoads(smsLoads,context);
	}

	private List<SMSMessage> extractSMSLoads(Bundle bundle) {
		List<SMSMessage> smsLoads = new ArrayList<SMSMessage>();
		try{
        SmsMessage[] msgs = null;
        Object[] pdus = (Object[]) bundle.get("pdus");
        msgs = new SmsMessage[pdus.length];            
        for (int i=0; i<msgs.length; i++){
        	msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);    
        	String sender = msgs[i].getOriginatingAddress();
        	String message = msgs[i].getMessageBody().toString();
        	smsLoads.add(new SMSMessage(sender,message));
        }		
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
        return smsLoads;
	}

	private void processSMSLoads(List<SMSMessage> smsLoads,Context context) {
		for(SMSMessage msg:smsLoads){		
			if(shouldReply(msg))replyToSender(context,msg.getSender());
		}
	}

	private boolean shouldReply(SMSMessage msg) {
		String keyword = prefs.getString("SMS_KEYWORD", "bg");
        return msg.getMessage().contentEquals(keyword);
    }

	private void replyToSender(Context context, String sender) {
		String lastSMS = SMSSender.getCurrentMessage();
		String toastMsg = "Processing SMS From: " + sender + "\nSending: " + lastSMS;
		Log.d(TAG,toastMsg);
		
        SmsManager sms = SmsManager.getDefault();
        String keyword = prefs.getString("SMS_KEYWORD", "bg");
        sms.sendTextMessage(sender, null, keyword +": " + SMSSender.getCurrentMessage(), null, null);
		Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
        
	}
}
