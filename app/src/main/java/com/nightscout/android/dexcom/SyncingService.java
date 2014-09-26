package com.nightscout.android.dexcom;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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
import com.nightscout.android.upload.Uploader;

import java.io.IOException;

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
        tracker.send(new HitBuilders.EventBuilder("Nightscout","startActionSingleSync").build());

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

            try {
                ReadData readData = new ReadData(mSerialDevice);
                EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
                MeterRecord[] meterRecords = readData.getRecentMeterRecords();

                int timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
                int nextUploadTime = TimeConstants.FIVE_MINUTES_MS - (timeSinceLastRecord * TimeConstants.SEC_TO_MS);
                int offset = 3000;

                Uploader uploader = new Uploader(mContext);
                uploader.upload(recentRecords, meterRecords);

                EGVRecord recentEGV = recentRecords[recentRecords.length - 1];
                broadcastSGVToUI(recentEGV, true, nextUploadTime + offset);
            } catch (Exception e) {
                Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
            }

            // Close serial
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                Tracker tracker;
                tracker = ((Nightscout) getApplicationContext()).getTracker();
                tracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("Unable to close serial connection")
                        .setFatal(false)
                        .build()
                );
                Log.e(TAG, "Unable to close", e);
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
        USBPower.PowerOn();
        try { Thread.sleep(3000); } catch (InterruptedException e) { }
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

    private void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus, int nextUploadTime) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, String.valueOf(egvRecord.getBGValue()) + " "
                                               + egvRecord.getTrendSymbol());
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

}