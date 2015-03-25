package com.nightscout.android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.drivers.BluetoothTransport;
import com.nightscout.android.drivers.USB.CdcAcmSerialDriver;
import com.nightscout.android.drivers.USB.UsbSerialProber;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.upload.Uploader;
import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.drivers.AbstractDevice;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.squareup.otto.Bus;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An {@link IntentService} subclass for handling asynchronous CGM Receiver downloads and cloud uploads
 * requests in a service on a separate handler thread.
 */
public class SyncingService extends IntentService {

    // Action for intent
    private static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";
    private static final String ACTION_POLL = "com.nightscout.android.dexcom.action.POLL";


    // Parameters for intent
    private static final String SYNC_PERIOD = "com.nightscout.android.dexcom.extra.SYNC_PERIOD";

    // Response to broadcast to activity
    public static final String RESPONSE_SGV = "mySGVMgdl";
    public static final String RESPONSE_TREND = "myTrend";
    public static final String RESPONSE_TIMESTAMP = "myTimestampMs";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTimeMs";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTimeMs";
    public static final String RESPONSE_JSON = "myJSON";
    public static final String RESPONSE_BAT = "myBatLvl";
    public static final String RESPONSE_PROTO = "myProtoDownload";
    public static final String RESPONSE_LAST_SGV_TIME = "lastSgvTimestamp";
    public static final String RESPONSE_LAST_METER_TIME = "lastMeterTimestamp";
    public static final String RESPONSE_LAST_SENSOR_TIME = "lastSensorTimestamp";
    public static final String RESPONSE_LAST_CAL_TIME = "lastCalTimestamp";

    private Bus bus = BusProvider.getInstance();

    private final String TAG = SyncingService.class.getSimpleName();

    // Constants
    private final int TIME_SYNC_OFFSET = 10000;
    public static final int MIN_SYNC_PAGES = 1;
    public static final int GAP_SYNC_PAGES = 20;


    /**
     * Starts this service to perform action Single Sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSingleSync(Context context, int numOfPages) {

        NightscoutPreferences preferences = new AndroidPreferences(context);

        // Exit if the user hasn't selected "I understand"
        if (!preferences.getIUnderstand()) {
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
                AndroidPreferences preferences = new AndroidPreferences(this);
                DeviceTransport driver = null;
                if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4) {
                    driver = UsbSerialProber.acquire(
                            (UsbManager) getSystemService(USB_SERVICE));
                } else if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE) {
                    driver = new BluetoothTransport(getApplicationContext());
                }


//                handleActionSync(param1, getApplicationContext(), driver);
                if (driver != null) {
                    handleActionSync(param1, getApplicationContext(), driver);
                } else {

                }
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    protected void handleActionSync(int numOfPages, Context context, DeviceTransport serialDriver) {
        EventReporter reporter = AndroidEventReporter.getReporter(context);
        AndroidPreferences preferences = new AndroidPreferences(context);
        Tracker tracker = ((Nightscout) context).getTracker();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
        wl.acquire();

        if (serialDriver != null) {
            AbstractUploaderDevice uploaderDevice = AndroidUploaderDevice.getUploaderDevice(context);
            AbstractDevice device = null;
            if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4 || preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE) {
                device = new DexcomG4(serialDriver, preferences, uploaderDevice);
                ((DexcomG4) device).setNumOfPages(numOfPages);
                if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4) {
                    ((CdcAcmSerialDriver) serialDriver).setPowerManagementEnabled(preferences.isRootEnabled());
                }
            }
            try {
//                DownloadResults results = device.download();
                G4Download download = (G4Download) device.download();

                Uploader uploader = new Uploader(context, preferences);
                boolean uploadStatus;
                if (numOfPages < 20) {
                    uploadStatus = uploader.upload(download, 1);
                } else {
                    uploadStatus = uploader.upload(download);
                }

                bus.post(download);
                bus.post(uploadStatus);
                reporter.report(EventType.DEVICE, EventSeverity.INFO,
                        getApplicationContext().getString(R.string.event_sync_log));
            } catch (ArrayIndexOutOfBoundsException e) {
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                        getApplicationContext().getString(R.string.event_fail_log));
                Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Array Index out of bounds")
                        .setFatal(false)
                        .build());
            } catch (NegativeArraySizeException e) {
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                        getApplicationContext().getString(R.string.event_fail_log));
                Log.wtf("Negative array exception from receiver", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Negative Array size")
                        .setFatal(false)
                        .build());
            } catch (IndexOutOfBoundsException e) {
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                        getApplicationContext().getString(R.string.event_fail_log));
                Log.wtf("IndexOutOfBounds exception from receiver", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("IndexOutOfBoundsException")
                        .setFatal(false)
                        .build());
            } catch (CRCFailError e) {
                // FIXME: may consider localizing this catch at a lower level (like ReadData) so that
                // if the CRC check fails on one type of record we can capture the values if it
                // doesn't fail on other types of records. This means we'd need to broadcast back
                // partial results to the UI. Adding it to a lower level could make the ReadData class
                // more difficult to maintain - needs discussion.
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                        getApplicationContext().getString(R.string.crc_fail_log));
                Log.wtf("CRC failed", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("CRC Failed")
                        .setFatal(false)
                        .build());
            } catch (Exception e) {
                reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                        context.getString(R.string.unknown_fail_log));
                Log.wtf("Unhandled exception caught", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Catch all exception in handleActionSync")
                        .setFatal(false)
                        .build());
            }
        }

//        if (!broadcastSent) broadcastSGVToUI();

        wl.release();
    }

    static public boolean isG4Connected(Context c) {
        // Iterate through devices and see if the dexcom is connected
        // Allowing us to start to start syncing if the G4 is already connected
        // vendor-id="8867" product-id="71" class="2" subclass="0" protocol="0"
        UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
        if (manager == null) return false;
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        boolean g4Connected = false;
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == 8867 && device.getProductId() == 71
                    && device.getDeviceClass() == 2 && device.getDeviceSubclass() == 0
                    && device.getDeviceProtocol() == 0) {
                g4Connected = true;
            }
        }
        return g4Connected;
    }

}