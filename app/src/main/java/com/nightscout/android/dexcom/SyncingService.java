package com.nightscout.android.dexcom;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.nightscout.android.MainActivity;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.dexcom.records.EGRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.upload.Uploader;

import java.io.IOException;

/**
 * An {@link IntentService} subclass for handling asynchronous Dexcom downloads and cloud uploads
 * requests in a service on a separate handler thread.
 */
public class SyncingService extends IntentService {

    private static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";

    private static final String TWO_DAY = "com.nightscout.android.dexcom.extra.2DAY";
    private static final String SINGLE = "com.nightscout.android.dexcom.extra.SINGLE";

    // Response to broadcast to activity
    public static final String RESPONSE_SGV = "mySGV";
    public static final String RESPONSE_TIMESTAMP = "myTimestamp";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";

    private final String TAG = SyncingService.class.getSimpleName();

    private Context mContext;
    private UsbManager mUsbManager;
    private UsbSerialDriver mSerialDevice;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSync(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SyncingService.class);
        intent.setAction(ACTION_SYNC);
        intent.putExtra(SINGLE, param1);
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
                final String param1 = intent.getStringExtra(TWO_DAY);
                handleActionSync(param1);
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSync(String param1) {
        acquireSerialDevice();
        ReadData readData = new ReadData(mSerialDevice);
        EGRecord[] recentRecords = readData.getRecentEGVs();
        MeterRecord[] meterRecords = readData.getRecentMeterRecords();
        Uploader uploader = new Uploader(mContext);
        uploader.upload(recentRecords, meterRecords);

        EGRecord recentEGV = recentRecords[recentRecords.length - 1];
        MeterRecord recentMeterBG = meterRecords[meterRecords.length - 1];
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, String.valueOf(recentEGV.getBGValue()) + " "
                + recentEGV.getTrendSymbol());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, recentEGV.getDisplayTime().toString());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, 60000*2.5);
        sendBroadcast(broadcastIntent);
    }

    // TODO: this needs to be more robust as before, but will clean up it and implement here, this
    // is just simple testing code
    private boolean acquireSerialDevice() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        if (mSerialDevice != null) {
            try {
                mSerialDevice.open();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Unable to acquire USB device from manager.");
        }
        return false;
    }

}
