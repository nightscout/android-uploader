package com.nightscout.android.dexcom;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nightscout.android.MQTT.MQTTMgr;
import com.nightscout.android.MainActivity;
import com.nightscout.android.SGV;
import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.upload.Uploader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous CGM Receiver downloads and cloud uploads
 * requests in a service on a separate handler thread.
 */
public class SyncingService extends IntentService {

    // Action for intent
    private static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";

    // Parameters for intent
    private static final String SYNC_PERIOD = "com.nightscout.android.dexcom.extra.SYNC_PERIOD";

    // Response to broadcast to activity
    public static final String RESPONSE_SGV = "mySGV";
    public static final String RESPONSE_TIMESTAMP = "myTimestamp";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";

    private final String TAG = SyncingService.class.getSimpleName();
    private Context mContext;
    private UsbManager mUsbManager;
    private UsbSerialDriver mSerialDevice;
    public static final String PROTOBUF_DOWNLOAD_TOPIC="/downloads/protobuf";

    /**
     * Starts this service to perform action Single Sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see android.app.IntentService
     */
    public static void startActionSingleSync(Context context, int numOfPages) {
        Intent intent = new Intent(context, SyncingService.class);
        intent.setAction(ACTION_SYNC);
        intent.putExtra(SYNC_PERIOD, numOfPages);
        context.startService(intent);
    }

    public SyncingService() {
        super("SyncingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)) {
                final int param1 = intent.getIntExtra(SYNC_PERIOD, 1);
                handleActionSync(param1);
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSync(int numOfPages) {

        if (acquireSerialDevice()) {

            ReadData readData = new ReadData(mSerialDevice);
            EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
            MeterRecord[] meterRecords = readData.getRecentMeterRecords();
            int batteryLevel = readData.readBatteryLevel();

            int timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
            int nextUploadTime = TimeConstants.FIVE_MINUTES_MS - (timeSinceLastRecord * TimeConstants.SEC_TO_MS);
            int offset = 3000;

            EGVRecord recentEGV = recentRecords[recentRecords.length - 1];

            // Close serial
            try {
                mSerialDevice.close();
                // Try powering off, will only work if rooted
                USBPower.PowerOff();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close and powerOff usb", e);
            }
            Uploader uploader = new Uploader(mContext);
            uploader.upload(recentRecords, meterRecords);
            publishToMQTT(recentEGV,batteryLevel);
            broadcastSGVToUI(recentEGV, true, nextUploadTime + offset);
        } else {
            // Not connected to serial device
            broadcastSGVToUI();
        }
    }

    public void publishToMQTT(EGVRecord recentEGV,int batteryLevel){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String url=sharedPref.getString("mqtt_endpoint","");
        String usr=sharedPref.getString("mqtt_user","");
        String pw=sharedPref.getString("mqtt_pass","");
        if (usr.equals("") || pw.equals("")){
            Log.w(TAG,"Username and/or password is not set for MQTT");
            return;
        }
        MQTTMgr mqttMgr = new MQTTMgr(getApplicationContext(),usr,pw,"dexcom");
        mqttMgr.connect(url);
        ArrayList<SGV> sgvs = new ArrayList<SGV>();
        SGV.ProposedCookieMonsterG4Download.Builder recordBuilder = SGV.ProposedCookieMonsterG4Download.newBuilder()
                .setDownloadStatus(SGV.ProposedCookieMonsterG4Download.DownloadStatus.SUCCESS)
                .setDownloadTimestamp(new Date().getTime())
                .setUploaderBattery((int) getBatteryLevel())
                .setReceiverBattery(batteryLevel)
                .setUnits(SGV.ProposedCookieMonsterG4Download.Unit.MGDL);
        SGV.ProposeCookieMonsterSGVG4 sgv = SGV.ProposeCookieMonsterSGVG4.newBuilder()
                .setSgv(recentEGV.getBGValue())
                .setTimestamp(recentEGV.getDisplayTime().getTime())
                .setDirection(recentEGV.getTrend().getProtoBuffEnum())
                .build();
        recordBuilder.addSgv(sgv);
        SGV.ProposedCookieMonsterG4Download download=recordBuilder.build();

        mqttMgr.publish(download.toByteArray(),PROTOBUF_DOWNLOAD_TOPIC);
        mqttMgr.disconnect();
    }

    // TODO: this needs to be more robust as before, but will clean up it and implement here, this
    // is just simple testing code
    private boolean acquireSerialDevice() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        if (mSerialDevice != null) {
            try {
                // Try powering on, will only work if rooted
                USBPower.PowerOn();
                mSerialDevice.open();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Unable to powerOn and open usb", e);
            }
        } else {
            Log.d(TAG, "Unable to acquire USB device from manager.");
        }
        return false;
    }

    private void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus, int nextUploadTime) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, String.valueOf(egvRecord.getBGValue()) + " "
                                               + egvRecord.getTrend().Symbol());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().toString());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastSGVToUI() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, "---");
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, "---");
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, TimeConstants.FIVE_MINUTES_MS);
        sendBroadcast(broadcastIntent);
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }
}