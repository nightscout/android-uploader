package com.nightscout.android.upload;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.nightscout.android.dexcom.EGVRecord;
import com.mongodb.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadHelper extends AsyncTask<EGVRecord, Integer, Long> {

    Context context;

    public UploadHelper(Context context) {
        this.context = context;
    }

    private String DB_URI = "";
    private String DB_COLLECTION = "";

    protected Long doInBackground(EGVRecord... data) {
        try {

            // connect to db
            MongoClientURI uri  = new MongoClientURI(DB_URI);
            MongoClient client = new MongoClient(uri);

            // get db
            DB db = client.getDB(uri.getDatabase());

            // get collection
            DBCollection dexcomData = db.getCollection(DB_COLLECTION);
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
        return 1L;
    }

    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        Log.i("Uploader", result + " Status: FINISHED");

    }

}
