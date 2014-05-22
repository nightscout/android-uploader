package com.nightscout.android.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.nightscout.android.dexcom.EGVRecord;
import android.preference.PreferenceManager;
import com.mongodb.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadHelper extends AsyncTask<EGVRecord, Integer, Long> {

    Context context;

    public UploadHelper(Context context) {
        this.context = context;
    }

    private String dbURI;
    private String collectionName;
    private Boolean switcher;
    private SharedPreferences prefs;

    protected Long doInBackground(EGVRecord... data) {

        prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        switcher = prefs.getBoolean("DatabaseSwitch", false);
        dbURI = prefs.getString("MongoDB URI", null);
        collectionName = prefs.getString("Collection Name", null);

        if (switcher && dbURI != null && collectionName != null) {
            try {

                // connect to db
                MongoClientURI uri  = new MongoClientURI(dbURI);
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());

                // get collection
                DBCollection dexcomData = db.getCollection(collectionName);
                Log.i("Uploader", "The number of EGV records being sent to MongoDB is " + data.length);
                for (int i = 0; i < data.length; i++) {
                    // make db object
                    BasicDBObject testData = new BasicDBObject();
                    testData.put("device", "dexcom");
                    SimpleDateFormat sdf  = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
                    Date date = sdf.parse(data[i].displayTime);
                    testData.put("date", date.getTime());
                    testData.put("dateString", data[i].displayTime);
                    testData.put("sgv", data[i].bGValue);
                    dexcomData.update(testData, testData, true, false, WriteConcern.UNACKNOWLEDGED);
                }

                client.close();
            }

            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
        return 1L;
    }

    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        Log.i("Uploader", result + " Status: FINISHED");

    }

}
