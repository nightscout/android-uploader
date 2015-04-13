package com.nightscout.android;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.drivers.BluetoothTransport;
import com.nightscout.android.drivers.USB.CdcAcmSerialDriver;
import com.nightscout.android.drivers.USB.UsbSerialProber;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.drivers.AbstractDevice;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.drivers.DeviceConnectionStatus;
import com.nightscout.core.drivers.DeviceState;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.utils.IsigReading;
import com.squareup.otto.Bus;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;

import java.io.IOException;

public class CollectorService extends Service {
//    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String TAG = CollectorService.class.getSimpleName();
    public static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";
    public static final String ACTION_POLL = "com.nightscout.android.dexcom.action.POLL";

    public static final String NUM_PAGES = "NUM_PAGES";
    public static final String STD_SYNC = "STD_SYNC";
    public static final String GAP_SYNC = "GAP_SYNC";
    public static final String SYNC_TYPE = "SYNC_TYPE";

    private EventReporter reporter;
    private AndroidPreferences preferences;
    private Tracker tracker;
    private PowerManager pm;
    private Bus bus = BusProvider.getInstance();
    protected AbstractDevice device = null;
    protected DeviceTransport driver;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;


    private AlarmManager alarmManager;
    private PendingIntent syncManager;

    private final IBinder mBinder = new LocalBinder();
    private long nextPoll = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        preferences = new AndroidPreferences(getApplicationContext());
        tracker = ((Nightscout) getApplicationContext()).getTracker();

        pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent syncIntent = new Intent(ACTION_SYNC);
        syncIntent.putExtra(NUM_PAGES, 1);
        syncIntent.putExtra(SYNC_TYPE, STD_SYNC);
        syncManager = PendingIntent.getBroadcast(getApplicationContext(), 1, syncIntent, 0);

        driver = null;
        setDriver();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getString(R.string.dexcom_device_type))) {
                    Log.d(TAG, "Interesting value changed! " + key);
                    if (driver != null && driver.isConnected()) {
                        try {
                            driver.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
//                    setDriver();
                } else {
                    Log.d(TAG, "Meh... something uninteresting changed");
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

    }

    public class LocalBinder extends Binder {
        public CollectorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CollectorService.this;
        }
    }

    private void setDriver() {
        SupportedDevices deviceType = preferences.getDeviceType();
        if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4 || preferences.getDeviceType() == SupportedDevices.UNKNOWN) {
            driver = UsbSerialProber.acquire(
                    (UsbManager) getSystemService(USB_SERVICE), getApplicationContext());
        } else if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE2) {
            driver = new BluetoothTransport(this);
        } else {
            throw new UnsupportedOperationException("Unsupported device selected");
        }
        AbstractUploaderDevice uploaderDevice = AndroidUploaderDevice.getUploaderDevice(getApplicationContext());
        if ((deviceType == SupportedDevices.DEXCOM_G4) || (deviceType == SupportedDevices.DEXCOM_G4_SHARE2)) {
            device = new DexcomG4(driver, preferences, uploaderDevice);
            device.setReporter(reporter);
            if (deviceType == SupportedDevices.DEXCOM_G4 && driver != null) {
                ((CdcAcmSerialDriver) driver).setPowerManagementEnabled(preferences.isRootEnabled());
                ((CdcAcmSerialDriver) driver).setUsbCriteria(DexcomG4.VENDOR_ID,
                        DexcomG4.PRODUCT_ID, DexcomG4.DEVICE_CLASS, DexcomG4.DEVICE_SUBCLASS,
                        DexcomG4.PROTOCOL);
            }
        }
//        try {
//            if (driver == null) {
//                Log.d(TAG, "Driver is NULL?!");
//                return;
//            }
//            if ((deviceType == SupportedDevices.DEXCOM_G4 && driver.isConnected())
//                    || deviceType == SupportedDevices.DEXCOM_G4_SHARE2) {
//                driver.open();
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "IOException: " + e.getMessage());
//            //TODO record this in the event log later
////            status = DownloadStatus.IO_ERROR;
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Log.d(TAG, this.getClass().getSimpleName() + " onDestory called");
            if (driver != null) {
                driver.close();
            } else {
                // TODO - find out why onDestory is being called on startup?
                Log.w(TAG, "Driver null. Why?");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        cancelPoll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
//        if (device == null) {
//            Log.d(TAG, "Device is null!");
//            ACRA.getErrorReporter().handleException(null);
//            return START_STICKY;
//        }
        if (intent == null) {
            Log.d(TAG, "Intent is null!");
//            ACRA.getErrorReporter().handleException(null);
            return START_STICKY;
        }
//        if (device.isConnected()) {
            int numOfPages = intent.getIntExtra(NUM_PAGES, 2);
            int syncType = intent.getStringExtra(SYNC_TYPE).equals(STD_SYNC) ? 0 : 1;
            new AsyncDownload().execute(numOfPages, syncType);
//        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class AsyncDownload extends AsyncTask<Integer, Integer, G4Download> {

        @Override
        protected G4Download doInBackground(Integer... params) {
            if (driver == null) {
                Log.w(TAG, "Driver is null");
                return null;
            }
            if (!device.isConnected()) {
                Log.e(TAG, "Device is not connected");
                try {
                    driver.open();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                Log.e(TAG, "Device is connected");
            }
            int numOfPages = params[0];
            String syncType = params[1] == 0 ? STD_SYNC : GAP_SYNC;
            ((DexcomG4) device).setNumOfPages(numOfPages);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
            wl.acquire();

            G4Download download = null;

            try {
                download = (G4Download) device.download();
                SensorRecord lastSensorRecord = null;
                CalRecord lastCalRecord = null;
                EGVRecord lastEgvRecord = null;
                long downloadEpoch = DateTime.parse(download.download_timestamp).getMillis();
                long rcvrTime = download.receiver_system_time_sec;
                if (download.sensor.size() > 0) {
                    lastSensorRecord = new SensorRecord(download.sensor.get(download.sensor.size() - 1), rcvrTime, downloadEpoch);
                } else {
                    Log.e(TAG, "No sensor records to calculate EGV using isig");
                }
                if (download.cal.size() > 0) {
                    lastCalRecord = new CalRecord(download.cal.get(download.cal.size() - 1), rcvrTime, downloadEpoch);
                } else {
                    Log.e(TAG, "No cal records to calculate EGV using isig");
                }
                if (download.sgv.size() > 0) {
                    lastEgvRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), rcvrTime, downloadEpoch);
                } else {
                    Log.e(TAG, "No egv records to calculate EGV using isig");
                }
                if (lastSensorRecord != null && lastCalRecord != null && lastEgvRecord != null) {
                    IsigReading isigReading = new IsigReading(lastSensorRecord, lastCalRecord, lastEgvRecord);
                    Log.e(TAG, "Calculated EGV using isig values: " + isigReading.asMgdl());
                }

                if (download != null) {
                    bus.post(download);
                }

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
                        getApplicationContext().getString(R.string.unknown_fail_log));
                Log.wtf("Unhandled exception caught", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Catch all exception in handleActionSync")
                        .setFatal(false)
                        .build());
            }
            wl.release();

            if (syncType.equals(GAP_SYNC)) {
                return download;
            }
            long nextUploadTime = Minutes.minutes(2).toStandardDuration().getMillis();
            if (download != null && download.sgv.size() > 0) {
                long rcvrTime = download.receiver_system_time_sec;
                long refTime = DateTime.parse(download.download_timestamp).getMillis();
                EGVRecord lastEgvRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), download.receiver_system_time_sec, refTime);
                nextUploadTime = Duration.standardSeconds(Minutes.minutes(5).toStandardSeconds().getSeconds() - ((rcvrTime - lastEgvRecord.getRawSystemTimeSeconds()) % Minutes.minutes(5).toStandardSeconds().getSeconds())).getMillis();
            }
            setNextPoll(nextUploadTime);
            return download;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public DeviceConnectionStatus getDeviceConnectionStatus() {
        if (device == null) {
            return new DeviceConnectionStatus(preferences.getDeviceType(), DeviceState.DISCONNECTED);
        }
        Log.d(TAG, "From service: " + device.getDeviceConnectionStatus().deviceType.name());
        return device.getDeviceConnectionStatus();
    }

    public long getNextPoll() {
//        return nextPoll - System.currentTimeMillis();
        return 1000 * 60 * 5 - (nextPoll - System.currentTimeMillis());
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setNextPoll(long millis) {
        Log.d(TAG, "Setting next poll with Alarm for " + millis + " ms from now.");
        nextPoll = System.currentTimeMillis() + millis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
        }
    }

    public void cancelPoll() {
        Log.d(TAG, "Canceling next alarm poll.");
        nextPoll = 0;
        alarmManager.cancel(syncManager);
    }

}
