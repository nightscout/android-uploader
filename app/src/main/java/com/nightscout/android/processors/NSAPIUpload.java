package com.nightscout.android.processors;

import android.content.Context;
import android.util.Log;

import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.processors.AbstractProcessor;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class NSAPIUpload extends AbstractProcessor {
    private static final String TAG = NSAPIUpload.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    private String apiSecret;
    private DefaultHttpClient httpclient;

    public NSAPIUpload(Context context, int deviceID){
        super(deviceID,context,"nightscout_uploader");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT );
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        httpclient = new DefaultHttpClient(params);
    }
    @Override
    public boolean process(G4Download d) {
        String postURL=sharedPref.getString("nsapi","");
        postURL += (postURL.endsWith("/") ? "" : "/") + "entries";
        Log.i(TAG, "Posting to: " + postURL);
        if (postURL=="") {
            return false;
        }
        int numRecs=d.getEGVRecords().size();
        int index=0;
        for (EGVRecord record : d.getEGVRecords()) {
            // TODO: Add filtering
            if (index < numRecs)
                continue;
            index+=1;
            try {
                HttpPost post = new HttpPost(postURL);
                JSONObject json = new JSONObject();
                json.put("device", "dexcom");
                json.put("date", record.getDisplayTimeSeconds());
                json.put("sgv", record.getBGValue());
                json.put("direction", record.getTrend().friendlyTrendName());
                String jsonString = json.toString();

                StringEntity se = new StringEntity(jsonString);
                post.setEntity(se);
                post.setHeader("Accept", "application/json");
                post.setHeader("Content-type", "application/json");
                apiSecret = sharedPref.getString("nskey","");
                MessageDigest md=MessageDigest.getInstance("SHA1");
                String apiSecretHash=byteArrayToHexString(md.digest(apiSecret.getBytes("UTF-8")));
                Log.d(TAG,"API Secret: "+apiSecretHash);
                post.setHeader("api-secret",apiSecretHash);
                /*
                POST /api/v1/entries/
                Accept: application/json
                Content-type: application/json
                api-secret: XXXXXX
                {"device":"dexcom","date":"1407695516000","sgv":"101","direction":"FortyFiveUp"}
                 */

                ResponseHandler responseHandler = new BasicResponseHandler();
                Log.d(TAG,"Post: "+post.getURI());

                String resp=(String) httpclient.execute(post, responseHandler);
                Log.d(TAG,"Response: "+resp);
            } catch (ClientProtocolException e) {
                Log.e(TAG,"Protocol exception during nightscout upload");
                break;
            } catch (JSONException e) {
                Log.e(TAG,"Exception during JSON operations");
            } catch (IOException e) {
                Log.e(TAG,"IOException during nightscout upload");
            } catch (NoSuchAlgorithmException e) {
                Log.d(TAG,"Unable to find SHA1 algorithm");
                break;
            }
        }
        return true;
    }

    protected static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
}