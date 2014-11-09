package com.nightscout.android.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.nightscout.android.MainActivity;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.dexcom.G4Constants;
import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.GlucoseUnit;
import com.nightscout.android.dexcom.Trend;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.processors.AbstractProcessor;
import com.nightscout.android.processors.AndroidNotificationProcessor;
import com.nightscout.android.processors.MQTTUploadProcessor;
import com.nightscout.android.processors.MongoProcessor;
import com.nightscout.android.processors.NSAPIUpload;
import com.nightscout.android.processors.PebbleProcessor;
import com.nightscout.android.processors.ProcessorChain;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public abstract class AbstractDevice implements DeviceInterface {
    private static final String TAG = AbstractDevice.class.getSimpleName();
    protected String name;
    protected int deviceID;
    protected GlucoseUnit unit=GlucoseUnit.MGDL;
    protected ProcessorChain chain=new ProcessorChain();
    // TODO: This download object is specific to the G4. This should probably be a more generic
    // Download object
    protected G4Download lastDownloadObject;
    protected Context context;
    protected DeviceTransportAbstract deviceTransport;
    protected boolean remote=false;
    //    protected Handler mHandler;
    protected String deviceType=null;
    protected String deviceIDStr;
    protected SharedPreferences sharedPref;
    protected boolean started=false;
    protected AlarmReceiver uiQuery;
    protected String driver;

    public static final String RESPONSE_SGV = "mySGV";
    public static final String RESPONSE_TREND = "myTrend";
    public static final String RESPONSE_TIMESTAMP = "myTimestamp";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTime";
    public static final String RESPONSE_JSON = "myJSON";
    public static final String RESPONSE_BAT = "myBatLvl";


    protected String phoneNum;

    public AbstractDevice(int deviceNum, Context context, String driver) {
        Log.i(TAG, "Creating " + name+"/"+driver);
        this.driver=driver;
        this.deviceID=deviceNum;
        this.context=context;
        this.deviceIDStr = "device_" + String.valueOf(deviceNum);
        sharedPref=PreferenceManager.getDefaultSharedPreferences(context);
        String contactDataUri=sharedPref.getString(deviceIDStr+"_contact_data_uri",Uri.EMPTY.toString());

        if (!contactDataUri.equals(Uri.EMPTY.toString()))
            phoneNum=getPhone(contactDataUri);
    }

    public String getContactNum() {
        return phoneNum;
    }

    public String getDeviceIDStr() {
        return deviceIDStr;
    }

    public void start(){
        Log.d(TAG,"Starting "+getName()+" (device_"+getDeviceID()+"/"+getDeviceType()+")");
        AbstractProcessor mon;

        Log.i(TAG, "Adding a Pebble monitor");
        chain.add(new PebbleProcessor(getName(), deviceID, getContext()));

        if (!isRemote()) {
            if (sharedPref.getBoolean("cloud_storage_mongodb_enable", false)) {
                Log.i(TAG, "Adding a mongo upload monitor");
                mon = new MongoProcessor(getContext(),deviceID);
                chain.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "cloud_storage_mqtt_enable", false)) {
                Log.i(TAG, "Adding a push notification upload monitor");
                mon = new MQTTUploadProcessor(getContext(),deviceID);
                chain.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "cloud_storage_api_enable", false)) {
                Log.i(TAG, "Adding a Nightscout upload monitor");
                mon = new NSAPIUpload(getContext(),deviceID);
                chain.add(mon);
            }
        } else {
            Log.i(TAG, "Ignoring processors that do not allow remote devices");
        }

        Log.i(TAG, "Adding a local android monitor");
        mon = new AndroidNotificationProcessor(deviceID, getContext());
        if (phoneNum!=null) {
            ((AndroidNotificationProcessor) mon).setPhoneNum(phoneNum);
        }
        chain.add(mon);
        chain.start();
        started=true;
        IntentFilter intentFilter=new IntentFilter(MainActivity.CGMStatusReceiver.REQUEST_DOWNLOAD);
        uiQuery=new AlarmReceiver();
        context.registerReceiver(uiQuery,intentFilter);
    }

    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public Context getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GlucoseUnit getUnit() {
        return unit;
    }

    public void stopProcessors(){
        chain.stop();
    }

    public void setUnit(GlucoseUnit unit) {
        this.unit = unit;
    }

    public boolean isConnected(){
        return deviceTransport.isOpen();
    }

    public void setContext(Context appContext) {
        this.context = appContext;
    }

    public void connect() throws IOException{
    }

    public void disconnect(){

    }

    public String getDeviceType() {
        if (deviceType==null)
            return "unknown";
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isRemote() {
        return remote;
    }

    public G4Download getLastDownloadObject(){
        return lastDownloadObject;
    }

    public void setLastDownloadObject(G4Download lastDownloadObject) {
        this.lastDownloadObject = lastDownloadObject;
    }


    protected void onDownload(G4Download dl){
        // convert into json for d3 plot
        JSONArray array = new JSONArray();
        for (EGVRecord record:dl.getEGVRecords()) array.put(record.toJSON());
        broadcastSGVToUI(dl.getLastEGVRecord(),true, dl.getNextUploadTime(),dl.getDisplayTime(),array,dl.getReceiverBattery());
        chain.process(dl);
//        GoogleAnalytics.getInstance(context.getApplicationContext()).dispatchLocalHits();
    }

    public void sendToUI(G4Download download){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, download.getLastEGV());
        broadcastIntent.putExtra(RESPONSE_TREND, download.getLastEGVTrend().ordinal());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, download.getLastEGVTimestamp());
//        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, download.getDownloadStatus().ordinal());
//        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, download.);
        JSONArray json = new JSONArray();
        for (EGVRecord record:download.getEGVRecords()) json.put(record.toJSON());
        broadcastIntent.putExtra(RESPONSE_JSON, json.toString());
        broadcastIntent.putExtra(RESPONSE_BAT, download.getReceiverBattery());
        context.sendStickyBroadcast(broadcastIntent);
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    @Override
    public void stop() {
        chain.stop();
        if (context !=null && uiQuery!=null)
            context.unregisterReceiver(uiQuery);
        started=false;
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MainActivity.CGMStatusReceiver.REQUEST_DOWNLOAD)){
                Log.d(TAG,"Received a query from the main activity for the download object");
                sendToUI(getLastDownloadObject());
            }
        }
    }

    private String getPhone(String uriString){
        return getPhone(Uri.parse(uriString));
    }

    private String getPhone(Uri dataUri){
        String id=dataUri.getLastPathSegment();
        Log.d(TAG,"id="+id);
        Log.d(TAG,"URI="+dataUri);
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, ContactsContract.Data._ID + " = ?", new String[]{id}, null);
        int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        Log.d(TAG, "cursor.getCount(): " + cursor.getCount());
        String phoneNum=null;
        if (cursor.moveToFirst()){
            phoneNum=cursor.getString(numIdx);
        }
        cursor.close();
        return phoneNum;
    }

    protected void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus,
                                  long nextUploadTime, long displayTime,
                                  JSONArray json, int batLvl) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, egvRecord.getBGValue());
        broadcastIntent.putExtra(RESPONSE_TREND, egvRecord.getTrend().getID());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().getTime());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, displayTime);
        if (json!=null)
            broadcastIntent.putExtra(RESPONSE_JSON, json.toString());
        broadcastIntent.putExtra(RESPONSE_BAT, batLvl);
        context.sendBroadcast(broadcastIntent);
    }

    protected void broadcastSGVToUI() {
        EGVRecord record=new EGVRecord(-1, Trend.NONE,new Date(),new Date());
        broadcastSGVToUI(record,false, (long) TimeConstants.FIVE_MINUTES_MS+ G4Constants.TIME_SYNC_OFFSET,new Date().getTime(),null,0);
    }

}