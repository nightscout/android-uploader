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

import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadHelper extends AsyncTask<EGVRecord, Integer, Long> {

    private static final String TAG = "DexcomUploadHelper";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;

    Context context;

    public UploadHelper(Context context) {
        this.context = context;
    }

    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    private SharedPreferences prefs;

    protected Long doInBackground(EGVRecord... records) {

        prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        enableRESTUpload = prefs.getBoolean("EnableRESTUpload", false);
        enableMongoUpload = prefs.getBoolean("EnableMongoUpload", false);

        if (enableRESTUpload) {
            doRESTUpload(prefs, records);
        }

        if (enableMongoUpload) {
            doMongoUpload(prefs, records);
        }

        return 1L;
    }

    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        Log.i(TAG, "Post execute, Result: " + result + ", Status: FINISHED");

    }

    private void doRESTUpload(SharedPreferences prefs, EGVRecord... records) {
        String postURI = prefs.getString("POST URI", null);

        try {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

            DefaultHttpClient httpclient = new DefaultHttpClient(params);

            HttpPost post = new HttpPost(postURI);

            for (EGVRecord record : records) {
                Date date = DATE_FORMAT.parse(record.displayTime);
                JSONObject json = new JSONObject();
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
                Log.i("Uploader", "The number of EGV records being sent to MongoDB is " + records.length);
                for (EGVRecord record : records) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
                    Date date = sdf.parse(record.displayTime);
                    testData.put("date", date.getTime());
                    testData.put("dateString", record.displayTime);
                    testData.put("sgv", record.bGValue);
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }
                client.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to upload data to mongo", e);
            }
        }

    }
}
