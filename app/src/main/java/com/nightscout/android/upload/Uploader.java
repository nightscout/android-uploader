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

import com.nightscout.android.MainActivity;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.GlucoseDataSet;
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
import java.util.ArrayList;
import java.util.Date;

public class Uploader {
    private static final String TAG = Uploader.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private Context mContext;
    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    private SharedPreferences prefs;

    public Uploader(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
        enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);
    }

    public boolean upload(GlucoseDataSet glucoseDataSet, MeterRecord meterRecord, CalRecord calRecord) {
        GlucoseDataSet[] glucoseDataSets = new GlucoseDataSet[1];
        glucoseDataSets[0] = glucoseDataSet;
        MeterRecord[] meterRecords = new MeterRecord[1];
        meterRecords[0] = meterRecord;
        CalRecord[] calRecords = new CalRecord[1];
        calRecords[0] = calRecord;
        return upload(glucoseDataSets, meterRecords, calRecords);
    }

    public boolean upload(GlucoseDataSet[] glucoseDataSets, MeterRecord[] meterRecords, CalRecord[] calRecords) {

        boolean mongoStatus = false;
        boolean apiStatus = false;

        if (enableRESTUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a REST API", glucoseDataSets.length));
            apiStatus = doRESTUpload(prefs, glucoseDataSets, meterRecords, calRecords);
            Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", glucoseDataSets.length, System.currentTimeMillis() - start));
        }

        if (enableMongoUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a Mongo", glucoseDataSets.length));
            mongoStatus = doMongoUpload(prefs, glucoseDataSets, meterRecords, calRecords);
            Log.i(TAG, String.format("Finished upload of %s record using a Mongo in %s ms", glucoseDataSets.length + meterRecords.length, System.currentTimeMillis() - start));
        }

        return apiStatus || mongoStatus;
    }

    private boolean doRESTUpload(SharedPreferences prefs, GlucoseDataSet[] glucoseDataSets, MeterRecord[] meterRecords, CalRecord[] calRecords) {
        String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
        ArrayList<String> baseURIs = new ArrayList<String>();

        try {
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to process API Base URL setting: " + baseURLSettings, e);
            return false;
        }

        for (String baseURI : baseURIs) {
            try {
                doRESTUploadTo(baseURI, glucoseDataSets, meterRecords, calRecords);
            } catch (Exception e) {
                Log.e(TAG, "Unable to do REST API Upload to: " + baseURI, e);
                return false;
            }
        }
        return true;
    }

    private void doRESTUploadTo(String baseURI, GlucoseDataSet[] glucoseDataSets, MeterRecord[] meterRecords, CalRecord[] calRecords) {
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

            for (GlucoseDataSet record : glucoseDataSets) {
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

                Log.i(TAG, "SGV JSON: " + jsonString);

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

            for (MeterRecord record : meterRecords) {
                JSONObject json = new JSONObject();

                try {
                    populateV1APIEntry(json, record);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                    continue;
                }

                String jsonString = json.toString();
                Log.i(TAG, "MBG JSON: " + jsonString);

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

            if (prefs.getBoolean("cloud_cal_data", false)) {
                for (CalRecord calRecord : calRecords) {

                    JSONObject json = new JSONObject();

                    try {
                        populateV1APIEntry(json, calRecord);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to populate entry, apiVersion: " + apiVersion, e);
                        continue;
                    }

                    String jsonString = json.toString();
                    Log.i(TAG, "CAL JSON: " + jsonString);

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
            }

            // TODO: this is a quick port from the original code and needs to be checked before release
            postDeviceStatus(baseURL, httpclient);

        } catch (Exception e) {
            Log.e(TAG, "Unable to post data", e);
        }
    }

    private void populateV1APIEntry(JSONObject json, GlucoseDataSet record) throws Exception {
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend().friendlyTrendName());
    }

    private void populateLegacyAPIEntry(JSONObject json, GlucoseDataSet record) throws Exception {
        json.put("device", "dexcom");
        json.put("date", record.getDisplayTime().getTime());
        json.put("sgv", Integer.parseInt(String.valueOf(record.getBGValue())));
        json.put("direction", record.getTrend().friendlyTrendName());
    }

    private void populateV1APIEntry(JSONObject json, MeterRecord record) throws Exception {
        json.put("device", "dexcom");
        json.put("type", "mbg");
        json.put("date", record.getDisplayTime().getTime());
        json.put("mbg", Integer.parseInt(String.valueOf(record.getMeterBG())));
    }

    private void populateV1APIEntry(JSONObject json, CalRecord record) throws Exception {
        json.put("device", "dexcom");
        json.put("type", "cal");
        json.put("date", record.getDisplayTime().getTime());
        json.put("slope", record.getSlope());
        json.put("intercept", record.getIntercept());
        json.put("scale", record.getScale());
    }

    // TODO: this is a quick port from original code and needs to be refactored before release
    private void postDeviceStatus(String baseURL, DefaultHttpClient httpclient) throws Exception {
        String devicestatusURL = baseURL + "devicestatus";
        Log.i(TAG, "devicestatusURL: " + devicestatusURL);

        JSONObject json = new JSONObject();
        json.put("uploaderBattery", MainActivity.batLevel);
        String jsonString = json.toString();

        HttpPost post = new HttpPost(devicestatusURL);
        StringEntity se = new StringEntity(jsonString);
        post.setEntity(se);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        ResponseHandler responseHandler = new BasicResponseHandler();
        httpclient.execute(post, responseHandler);
    }

    private boolean doMongoUpload(SharedPreferences prefs, GlucoseDataSet[] glucoseDataSets,
                               MeterRecord[] meterRecords, CalRecord[] calRecords) {

        String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
        String collectionName = prefs.getString("cloud_storage_mongodb_collection", null);
        String dsCollectionName = prefs.getString("cloud_storage_mongodb_device_status_collection", "devicestatus");

        if (dbURI != null && collectionName != null) {
            try {

                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI.trim());
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());

                // get collection
                DBCollection dexcomData = db.getCollection(collectionName.trim());
                Log.i(TAG, "The number of EGV records being sent to MongoDB is " + glucoseDataSets.length);
                for (GlucoseDataSet record : glucoseDataSets) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    testData.put("date", record.getDisplayTime().getTime());
                    testData.put("dateString", record.getDisplayTime().toString());
                    testData.put("sgv", record.getBGValue());
                    testData.put("direction", record.getTrend().friendlyTrendName());
                    if (prefs.getBoolean("cloud_sensor_data", false)) {
                        testData.put("filtered", record.getFiltered());
                        testData.put("unfilterd", record.getUnfiltered());
                        testData.put("rssi", record.getRssi());
                    }
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }

                Log.i(TAG, "The number of MBG records being sent to MongoDB is " + meterRecords.length);
                for (MeterRecord meterRecord : meterRecords) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    testData.put("type", "mbg");
                    testData.put("date", meterRecord.getDisplayTime().getTime());
                    testData.put("dateString", meterRecord.getDisplayTime().toString());
                    testData.put("mbg", meterRecord.getMeterBG());
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }

                // TODO: might be best to merge with the the glucose data but will require time
                // analysis to match record with cal set, for now this will do
                if (prefs.getBoolean("cloud_cal_data", false)) {
                    for (CalRecord calRecord : calRecords) {
                        // make db object
                        BasicDBObject testData = new BasicDBObject();
                        testData.put("device", "dexcom");
                        testData.put("date", calRecord.getDisplayTime().getTime());
                        testData.put("dateString", calRecord.getDisplayTime().toString());
                        testData.put("slope", calRecord.getSlope());
                        testData.put("intercept", calRecord.getIntercept());
                        testData.put("scale", calRecord.getScale());
                        dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                    }
                }

                // TODO: quick port from original code, revisit before release
                DBCollection dsCollection = db.getCollection(dsCollectionName);
                BasicDBObject devicestatus = new BasicDBObject();
                devicestatus.put("uploaderBattery", MainActivity.batLevel);
                devicestatus.put("created_at", new Date());
                dsCollection.insert(devicestatus, WriteConcern.UNACKNOWLEDGED);

                client.close();

                return true;

            } catch (Exception e) {
                Log.e(TAG, "Unable to upload data to mongo", e);
            }
        }
        return false;
    }
}
