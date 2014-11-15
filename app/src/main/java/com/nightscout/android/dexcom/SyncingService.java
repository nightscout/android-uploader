package com.nightscout.android.dexcom;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.R;
import com.nightscout.android.devices.AbstractDevice;
import com.nightscout.android.devices.DeviceTransportAbstract;
import com.nightscout.android.devices.DexcomG4;
import com.nightscout.android.devices.USBSerialTransport;
import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.dexcom.records.SensorRecord;
import com.nightscout.android.processors.ProcessorChain;
import org.json.JSONArray;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
    public static final String RESPONSE_TREND = "myTrend";
    public static final String RESPONSE_TIMESTAMP = "myTimestamp";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTime";
    public static final String RESPONSE_JSON = "myJSON";
    public static final String RESPONSE_BAT = "myBatLvl";
    private boolean deviceInitialized = false;

    private final String TAG = SyncingService.class.getSimpleName();

    /**
     * Starts this service to perform action Single Sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSingleSync(Context context, int numOfPages) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Exit if the user hasn't selected "I understand"
        if (! prefs.getBoolean("i_understand",false)) {
            Toast.makeText(context, R.string.message_user_not_understand, Toast.LENGTH_LONG).show();
            return;
        }

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
        AbstractDevice device = null;
        if (! deviceInitialized) {
            Log.i("XXX","Initializing device");
            device = new DexcomG4(1, getApplicationContext());
            device.start();
            deviceInitialized=true;
        }
        if (device!=null) {
            Log.i("XXX","Device initialized: "+device.getDriver());
            ((DexcomG4) device).download();
        }
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
            if (device.getVendorId() == 8867 && device.getProductId() == 71
                    && device.getDeviceClass() == 2 && device.getDeviceSubclass() ==0
                    && device.getDeviceProtocol() == 0){
                g4Connected=true;
            }
        }
        return g4Connected;
    }
}
