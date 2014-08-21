//1.2
package com.nightscout.android.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mongodb.*;
import com.nightscout.android.dexcom.DexcomG4Service;
import com.nightscout.android.dexcom.EGVRecord;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.json.JSONObject;
import org.marre.SmsSender;
import org.marre.sms.SmsException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadHelper extends AsyncTask<EGVRecord, Integer, Long> {


    private static final String TAG = "DexcomUploadHelper";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa",Locale.US);
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    
    private static final int RESET_VALUE = 0;
    private static final int INTERVAL_MET_MARKER = 1;
    
    private static int smsSkipCount = RESET_VALUE;
    private static int inRangeSkipCount = RESET_VALUE;
    private static int noDataSkipCount = RESET_VALUE;
    
    private static Integer previousBg = 0;
    private static String previousDisplayTime = "";

    private static final String CLICKATELL_URL = "http://api.clickatell.com/http/sendmsg?api_id=%s&user=%s&password=%s&to=%s&text=%s&from=%s&mo=1";
    Context context;



    public UploadHelper(Context context) {
        this.context = context;
    }


    protected Long doInBackground(EGVRecord... records) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        Boolean enableRESTUpload = prefs.getBoolean("EnableRESTUpload", false);
        Boolean enableMongoUpload = prefs.getBoolean("EnableMongoUpload", false);
        Boolean enableSMS = prefs.getBoolean("SMS_ENABLE", false);

        if (enableRESTUpload) {
            doRESTUpload(prefs, records);
        }

        if (enableMongoUpload) {
            doMongoUpload(prefs, records);
        }
        
        if(!DexcomG4Service.isInitialRead() && enableSMS){
        	processForSMS(records);
        }
        
        return 1L;
    }

    private void processForSMS(EGVRecord[] records) {

        for (EGVRecord record : records) {

        	if(record.displayTime.contentEquals(previousDisplayTime)){
        		noDataSkipCount = processSkipCount(noDataSkipCount,getIntPreferenceFromString(context,"NO_DATA_INTERVAL"));
        	     if(skipCountMet(noDataSkipCount)){
        	    	 sendSMSHttp(context,"No data since " + previousDisplayTime);
        	     }
            	return;
        	}
        	
        	//Valid data point recieved, reset no data skip count;
        	noDataSkipCount = RESET_VALUE;

        	int BGInt = Integer.parseInt(record.bGValue);
            if (shouldSendSMS(BGInt)) sendDataPointSMS(record, BGInt, records.length);

            //store current values
            previousBg = BGInt;
            previousDisplayTime = record.displayTime;
        }		
	}


	protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        Log.i(TAG, "Post execute, Result: " + result + ", Status: FINISHED");

    }

    private void doRESTUpload(SharedPreferences prefs, EGVRecord... records) {
        try {
            String baseURL = prefs.getString("API Base URL", "");
            String postURL = baseURL + (baseURL.endsWith("/") ? "" : "/") + "entries";
            Log.i(TAG, "postURL: " + postURL);

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

            DefaultHttpClient httpclient = new DefaultHttpClient(params);

            HttpPost post = new HttpPost(postURL);

            for (EGVRecord record : records) {
                Date date = DATE_FORMAT.parse(record.displayTime);
                JSONObject json = new JSONObject();
                json.put("device", "dexcom");
                json.put("timestamp", date.getTime());
                json.put("bg", Integer.parseInt(record.bGValue));
                json.put("direction", record.trend);

                String jsonString = json.toString();

                Log.i(TAG, "DEXCOM JSON: " + jsonString);

                try {
                    StringEntity se = new StringEntity(jsonString);
                    post.setEntity(se);
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-type", "application/json");

                    ResponseHandler responseHandler = new BasicResponseHandler();
                    httpclient.execute(post, responseHandler);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to post data to: '" + post.getURI().toString() + "'", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to post data", e);
        }
    }

    private void doMongoUpload(SharedPreferences prefs, EGVRecord... records) {

        String dbURI = prefs.getString("MongoDB URI", null);
        String collectionName = prefs.getString("Collection Name", null);

        if (dbURI != null && collectionName != null) {
            try {

                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI);
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());

                // get collection
                DBCollection dexcomData = db.getCollection(collectionName);
                Log.i(TAG, "The number of EGV records being sent to MongoDB is " + records.length);
                for (EGVRecord record : records) {
                    // make db object
                    Date date = DATE_FORMAT.parse(record.displayTime);
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    testData.put("date", date.getTime());
                    testData.put("dateString", record.displayTime);
                    testData.put("sgv", record.bGValue);
                    testData.put("direction", record.trend);
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }
                client.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to upload data to mongo", e);
            }
        }
        
    }

    private boolean shouldSendSMS(int BGInt){
        // First Data Point
        if (previousBg == 0) return false;

        // Severe hypo. Send every data point until over 55.
        if(BGInt < 55) return true;

        //Load BG thresholds from preference
        int SMSBgHigh = getIntPreferenceFromString(context,"SMSBgHigh");
        int SMSBgLow = getIntPreferenceFromString(context,"SMSBgLow");
            
        // BG in range.  Do not send text. Reset skipCount to reset value.
        if(BGInt >= SMSBgLow && BGInt <= SMSBgHigh){
            smsSkipCount = RESET_VALUE;
            inRangeSkipCount = processSkipCount(inRangeSkipCount,getIntPreferenceFromString(context,"IN_RANGE_INTERVAL"));
            return skipCountMet(inRangeSkipCount);
        }

    	// BG is out of range, reset in-range skip count
        inRangeSkipCount = RESET_VALUE;
        
        //Process skip count
        smsSkipCount = processSkipCount(smsSkipCount,getIntPreferenceFromString(context,"DATA_POINT_SMS_INTERVAL"));
        
        //Return true if skip count is met
        return skipCountMet(smsSkipCount);
        
    }
    private static int getIntPreferenceFromString(Context context, String key){
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String stringValue = prefs.getString(key, String.valueOf(RESET_VALUE));
        int value = RESET_VALUE;
        try{
       	 value = Integer.valueOf(stringValue);
        }catch(Exception e){
        }
        return value;
        
   }
    private static String getStringPreference(Context context,String key){
	   	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, "");
   }
 
    
    private boolean skipCountMet(int count){
    	//Return true if counter has met the interval met marker
    	return (count == INTERVAL_MET_MARKER);
    }
    
	 private int processSkipCount(int count, int interval){
	        //Counter hasn't reached interval value, increase counter
		 if(count < interval){  	
	     	count++;
		 }else{	
			 //Counter met, reset value
			 count = RESET_VALUE;
	    }
	    return count;
	 }
    
	private void sendDataPointSMS(EGVRecord record, int BGInt, int recordLength) {
		String bgMessage = getBGMessage(record.bGValue, BGInt);
		String SMStime = getTimeStamp(record.displayTime);

		String SMSmsg = SMStime + " - " + bgMessage;
		
		sendSMSHttp(context,SMSmsg);

	}

	private String getBGMessage(String stringBGValue, int intBGInt) {
		String bgMessage = "";

        if(previousBg == 0) {
            bgMessage = "Connected - Awaiting first reading...";
        }else if (intBGInt == previousBg){
            bgMessage = stringBGValue + " stable ";
        }else if(intBGInt > previousBg){
            bgMessage = stringBGValue + " up " + (intBGInt - previousBg);
        }else if(intBGInt < previousBg){
            bgMessage = stringBGValue + " down " + (previousBg - intBGInt);
        }

        if (intBGInt < 55){
            bgMessage = bgMessage + " - UNDER 55";
        }

        return bgMessage;
	}
	
	private String getTimeStamp(String displayTime ) {
		String SMStime = displayTime;

		if (SMStime.substring(11,12).equals("0"))
		{SMStime = SMStime.substring(12,16) + SMStime.substring(19,22);}
		else
		{SMStime = SMStime.substring(11,16) + SMStime.substring(19,22);}
		
		return SMStime;
	}

	
	public static void sendSMSHttpAsync(final Context context, final String msg){
		new AsyncTask<Void,Void,Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				sendSMSHttp(context,msg);
				return null;
			}}.execute();
		
	}
	
	public static void sendSMSHttp(Context context,String msg) {

	    String CLICKATELL_USERNAME = getStringPreference(context,"CLICKATELL_USERNAME"); // Your Clickatell username
	    String CLICKATELL_PASSWORD = getStringPreference(context,"CLICKATELL_PASSWORD");
	    String CLICKATELL_APIID = getStringPreference(context,"CLICKATELL_APIID");
	    String SMS_SENDER = getStringPreference(context,"SMS_SENDER");
	    String SMS_RECEIVER = getStringPreference(context,"SMS_RECEIVER");
	    
       String url = String.format(CLICKATELL_URL, CLICKATELL_APIID,CLICKATELL_USERNAME, CLICKATELL_PASSWORD,SMS_RECEIVER,Uri.encode(msg),SMS_SENDER);

        HttpParams params = new BasicHttpParams();
	            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
	            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
	            
	         DefaultHttpClient httpclient = new DefaultHttpClient(params);

	         HttpGet get = new HttpGet(url);
	         try {
				httpclient.execute(get);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
