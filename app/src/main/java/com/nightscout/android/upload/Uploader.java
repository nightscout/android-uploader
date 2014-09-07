package com.nightscout.android.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.nightscout.android.dexcom.records.EGRecord;
import com.nightscout.android.dexcom.records.MeterRecord;

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

public class Uploader {
    private static final String TAG = Uploader.class.getSimpleName();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    private Context mContext;
    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    private SharedPreferences prefs;

    public Uploader(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        enableRESTUpload = prefs.getBoolean("EnableRESTUpload", false);
        enableMongoUpload = prefs.getBoolean("EnableMongoUpload", false);
    }

    public boolean upload(EGRecord[] egRecords, MeterRecord[] meterRecords) {
        if (enableRESTUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a REST API", egRecords.length));
            doRESTUpload(prefs, egRecords);
            Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", egRecords.length, System.currentTimeMillis() - start));
        }

        if (enableMongoUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a Mongo", egRecords.length));
            doMongoUpload(prefs, egRecords, meterRecords);
            Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms", egRecords.length + meterRecords.length, System.currentTimeMillis() - start));
        }
        return true;
    }

    private void doRESTUpload(SharedPreferences prefs, EGRecord... records) {
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

    private void doRESTUploadTo(String baseURI, EGRecord[] records) {
        try {
            int apiVersion = 0;
            if (baseURI.endsWith("/v1/")) apiVersion = 1;

            String baseURL = null;
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

            for (EGRecord record : records) {
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

    private void populateV1APIEntry(JSONObject json, EGRecord record) throws Exception {
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend());
    }

    private void populateLegacyAPIEntry(JSONObject json, EGRecord record) throws Exception {
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend());
    }

    private void doMongoUpload(SharedPreferences prefs, EGRecord[] egRecords, MeterRecord[] meterRecords) {

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
                Log.i(TAG, "The number of EGV records being sent to MongoDB is " + egRecords.length);
                for (EGRecord record : egRecords) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    testData.put("date", record.getDisplayTime().getTime());
                    testData.put("dateString", record.getDisplayTime().toString());
                    testData.put("sgv", record.getBGValue());
                    testData.put("direction", record.getTrend());
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }
                Log.i(TAG, "The number of MBG records being sent to MongoDB is " + meterRecords.length);
                for (MeterRecord meterRecord : meterRecords) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    testData.put("date", meterRecord.getDisplayTime().getTime());
                    testData.put("dateString", meterRecord.getDisplayTime().toString());
                    testData.put("mbg", meterRecord.getMeterBG());
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }
                client.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to upload data to mongo", e);
            }
        }

    }
}
