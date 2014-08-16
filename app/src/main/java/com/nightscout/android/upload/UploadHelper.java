package com.nightscout.android.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mongodb.*;
import com.nightscout.android.dexcom.EGVRecord;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UploadHelper extends AsyncTask<EGVRecord, Integer, Long> {

    private static final String TAG = "DexcomUploadHelper";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    private static final int BGV_MAGIC_HOURGLASS = 9;
    private static final int BGV_MAGIC_QUESTION = 10;
    private static final int BGV_MAGIC_TEST = 14;

    Context context;

    public UploadHelper(Context context) {
        this.context = context;
    }

    protected Long doInBackground(EGVRecord... records) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        Boolean enableRESTUpload = prefs.getBoolean("EnableRESTUpload", false);
        Boolean enableMongoUpload = prefs.getBoolean("EnableMongoUpload", false);
        Boolean enablePushover = prefs.getBoolean("EnablePushover", false);

        if (enableRESTUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a REST API", records.length));
            doRESTUpload(prefs, records);
            Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", records.length, System.currentTimeMillis() - start));
        }

        if (enableMongoUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a Mongo", records.length));
            doMongoUpload(prefs, records);
            Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms", records.length, System.currentTimeMillis() - start));
        }

        if (enablePushover) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting Pushover notification of %s record", records.length));
            doPushoverNotification(prefs, records);
            Log.i(TAG, String.format("Finished Pushover notification of %s in %s ms", records.length, System.currentTimeMillis() - start));
        }

        return 1L;
    }

    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        Log.i(TAG, "Post execute, Result: " + result + ", Status: FINISHED");

    }

    private void doRESTUpload(SharedPreferences prefs, EGVRecord... records) {
        String baseURLSettings = prefs.getString("API Base URL", "");
        ArrayList<String> baseURIs = new ArrayList<String>();

        try {
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to process API Base URL setting: " + baseURLSettings, e);
            return;
        }

        for (String baseURI : baseURIs) {
            try {
                doRESTUploadTo(baseURI, records);
            } catch (Exception e) {
                Log.e(TAG, "Unable to do REST API Upload to: " + baseURI, e);
            }
        }
    }

    private void doRESTUploadTo(String baseURI, EGVRecord[] records) {
        try {
            int apiVersion = 0;
            if (baseURI.endsWith("/v1/")) apiVersion = 1;

            String baseURL;
            String secret = null;
            String[] uriParts = baseURI.split("@");

            if (uriParts.length == 1 && apiVersion == 0) {
                baseURL = uriParts[0];
            } else if (uriParts.length == 1 && apiVersion > 0) {
                throw new Exception("Starting with API v1, a pass phase is required");
            } else if (uriParts.length == 2 && apiVersion > 0) {
                secret = uriParts[0];
                baseURL = uriParts[1];
            } else {
                throw new Exception(String.format("Unexpected baseURI: %s, uriParts.length: %s, apiVersion: %s", baseURI, uriParts.length, apiVersion));
            }

            String postURL = baseURL + "entries";
            Log.i(TAG, "postURL: " + postURL);

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

            DefaultHttpClient httpclient = new DefaultHttpClient(params);

            HttpPost post = new HttpPost(postURL);

            if (apiVersion > 0) {
                if (secret == null || secret.isEmpty()) {
                    throw new Exception("Starting with API v1, a pass phase is required");
                } else {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] bytes = secret.getBytes("UTF-8");
                    digest.update(bytes, 0, bytes.length);
                    bytes = digest.digest();
                    StringBuilder sb = new StringBuilder(bytes.length * 2);
                    for (byte b: bytes) {
                        sb.append(String.format("%02x", b & 0xff));
                    }
                    String token = sb.toString();
                    post.setHeader("api-secret", token);
                }
            }

            for (EGVRecord record : records) {
                JSONObject json = new JSONObject();

                try {
                    if (apiVersion >= 1)
                        populateV1APIEntry(json, record);
                    else
                        populateLegacyAPIEntry(json, record);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                    continue;
                }

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

    private void populateV1APIEntry(JSONObject json, EGVRecord record) throws Exception {
        Date date = DATE_FORMAT.parse(record.displayTime);
        json.put("device", "dexcom");
        json.put("date", date.getTime());
        json.put("sgv", Integer.parseInt(record.bGValue));
        json.put("direction", record.trend);
    }

    private void populateLegacyAPIEntry(JSONObject json, EGVRecord record) throws Exception {
        Date date = DATE_FORMAT.parse(record.displayTime);
        json.put("device", "dexcom");
        json.put("timestamp", date.getTime());
        json.put("bg", Integer.parseInt(record.bGValue));
        json.put("direction", record.trend);
    }

    private void doMongoUpload(SharedPreferences prefs, EGVRecord... records) {

        String dbURI = prefs.getString("MongoDB URI", null);
        String collectionName = prefs.getString("Collection Name", null);

        if (dbURI != null && collectionName != null) {
            try {

                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI.trim());
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());

                // get collection
                DBCollection dexcomData = db.getCollection(collectionName.trim());
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

    private void handlePushoverError(Exception e, String message) {
        Log.e(TAG, message, e);
    }

    private void doPushoverNotification(SharedPreferences prefs, EGVRecord... records) {

        String apiToken = prefs.getString("PushoverAPIToken", null);
        String userKey = prefs.getString("PushoverUserKey", null);
        int lowThreshold = Integer.valueOf(prefs.getString("PushoverHighThreshold", "-1"));
        int highThreshold = Integer.valueOf(prefs.getString("PushoverLowThreshold", "-1"));
        Boolean alertProblems = prefs.getBoolean("PushoverAlertProblems", false);
        int priority = prefs.getBoolean("PushoverContinuous", false) ? 2 : 1;

        String title;
        String message;

        if (apiToken != null && userKey != null && lowThreshold != -1 && highThreshold != -1) {
            Pushover push = new Pushover(apiToken, userKey);

            for (EGVRecord record : records) {
                title = null;
                message = null;

                try {
                    int bGValue = Integer.parseInt(record.bGValue);

                    switch (bGValue) {
                        case BGV_MAGIC_HOURGLASS:
                        case BGV_MAGIC_QUESTION:
                            if (alertProblems) {
                                title = (bGValue == BGV_MAGIC_HOURGLASS ? "Hourglass" : "???") + " Detected";
                                message = "Please check your GCM for connectivity issues.";
                            }
                            break;
                        case BGV_MAGIC_TEST:
                            title = "Nightscout Test Notification";
                            message = "It works!";
                            break;
                        default:
                            if (bGValue <= lowThreshold) {
                                title = "Low Glucose Reading";
                            } else if (bGValue >= highThreshold) {
                                title = "High Glucose Reading";
                            }

                            message =  record.trendArrow + " A blood glucose value of " + record.bGValue + " was logged at " + record.displayTime + ".";
                    }


                    if (title != null) {
                        PushoverResponse response = push.sendMessage(message, title, priority);

                        if (response.getStatus() != 1) {
                            for (String error : response.getErrors()) {
                                handlePushoverError(null, "An error was returned by Pushover API: " + error);
                            }
                        }
                    }

                } catch (Exception e) {
                    handlePushoverError(e, "Error sending Pushover notification");
                }
            }
        }

    }
}
