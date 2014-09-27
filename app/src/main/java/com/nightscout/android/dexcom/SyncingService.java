package com.nightscout.android.dexcom;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.dexcom.records.SensorRecord;
import com.nightscout.android.upload.Uploader;

import org.acra.ACRA;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * An {@link IntentService} subclass for handling asynchronous CGM Receiver downloads and cloud uploads
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
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTime";
    public static final String RESPONSE_RSSI = "myRSSI";
    public static final String RESPONSE_BAT = "myBatLvl";

    private final String TAG = SyncingService.class.getSimpleName();
    private Context mContext;
    private UsbManager mUsbManager;
    private UsbSerialDriver mSerialDevice;


    /**
     * Starts this service to perform action Single Sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSingleSync(Context context, int numOfPages) {
        Tracker tracker=((Nightscout) context).getTracker();
        tracker.send(new HitBuilders.EventBuilder("DexcomG4", "Sync").setValue(numOfPages).build());

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
        Tracker tracker = ((Nightscout) getApplicationContext()).getTracker();
        USBPower.PowerOn();
        try { Thread.sleep(3000); } catch (InterruptedException e) { }

        if (acquireSerialDevice()) {
            try {
                ReadData readData = new ReadData(mSerialDevice);
                EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
                MeterRecord[] meterRecords = readData.getRecentMeterRecords();
                SensorRecord[] sensorRecords = readData.getRecentSensorRecords();

                // FIXME: should we do boundary checking here as well?
                int timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
                int nextUploadTime = TimeConstants.FIVE_MINUTES_MS - (timeSinceLastRecord * TimeConstants.SEC_TO_MS);
                long displayTime = readData.readDisplayTime().getTime();
                int rssi = sensorRecords[sensorRecords.length-1].getRSSI();
                int batLevel = readData.readBatteryLevel();

                // Close serial
                mSerialDevice.close();

                Uploader uploader = new Uploader(mContext);
                uploader.upload(recentRecords, meterRecords);

                // TODO: should this be a constant?
                int offset = 3000;
                EGVRecord recentEGV = recentRecords[recentRecords.length - 1];
                broadcastSGVToUI(recentEGV, true, nextUploadTime + offset, displayTime, rssi,batLevel);

            } catch (IOException e) {
                tracker.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("Unable to close serial connection")
                                .setFatal(false)
                                .build()
                );
                Log.e(TAG, "Unable to close", e);
            } catch (Exception e) {
                Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
                ACRA.getErrorReporter().handleException(e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Catch all exception in handleActionSync")
                        .setFatal(false)
                        .build());
            }
            // Try powering off, will only work if rooted
            USBPower.PowerOff();
        } else {
            // Not connected to serial device
            broadcastSGVToUI();
        }
    }

    // TODO: this needs to be more robust as before, but will clean up it and implement here, this
    // is just simple testing code
    private boolean acquireSerialDevice() {
        // Try powering on, will only work if rooted
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        if (mSerialDevice != null) {
            try {
                mSerialDevice.open();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Unable to powerOn and open usb", e);
                Tracker tracker;
                tracker = ((Nightscout) getApplicationContext()).getTracker();
                tracker.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("Unable to open serial connection")
                                .setFatal(false)
                                .build()
                );
            }
        } else {
            Log.d(TAG, "Unable to acquire USB device from manager.");
        }
        return false;
    }

    static public boolean isG4Connected(Context c){
        // Iterate through devices and see if the dexcom is connected
        // Allowing us to start to start syncing if the G4 is already connected
        // vendor-id="8867" product-id="71" class="2" subclass="0" protocol="0"
        UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        boolean g4Connected=false;
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId()==8867 && device.getProductId()==71
                    && device.getDeviceClass()==2 && device.getDeviceSubclass()==0
                    && device.getDeviceProtocol()== 0){
                g4Connected=true;
            }
        }
        return g4Connected;
    }

    private void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus, int nextUploadTime,long displayTime, int rssi, int batLvl) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, String.valueOf(egvRecord.getBGValue()) + " "
                + egvRecord.getTrendSymbol());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().toString());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, displayTime);
        broadcastIntent.putExtra(RESPONSE_RSSI, rssi);
        broadcastIntent.putExtra(RESPONSE_BAT, batLvl);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastSGVToUI() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, "---");
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, "---");
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, TimeConstants.FIVE_MINUTES_MS);
        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, new Date().getTime());
        broadcastIntent.putExtra(RESPONSE_RSSI, -1);
        broadcastIntent.putExtra(RESPONSE_BAT, -1);
        sendBroadcast(broadcastIntent);
    }

}