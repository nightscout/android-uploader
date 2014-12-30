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
import com.nightscout.android.drivers.USB.CdcAcmSerialDriver;
import com.nightscout.android.drivers.USB.UsbSerialProber;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.upload.Uploader;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.drivers.AbstractDevice;
import com.nightscout.core.drivers.AbstractUploaderDevice;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.CookieMonsterDownload;
import com.nightscout.core.model.CookieMonsterG4Cal;
import com.nightscout.core.model.CookieMonsterG4Meter;
import com.nightscout.core.model.CookieMonsterG4SGV;
import com.nightscout.core.model.CookieMonsterG4Sensor;
import com.nightscout.core.model.DownloadResults;
import com.nightscout.core.model.Noise;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.joda.time.Duration.standardMinutes;

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
    public static final String RESPONSE_SGV = "mySGVMgdl";
    public static final String RESPONSE_TREND = "myTrend";
    public static final String RESPONSE_TIMESTAMP = "myTimestampMs";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTimeMs";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTimeMs";
    public static final String RESPONSE_JSON = "myJSON";
    public static final String RESPONSE_BAT = "myBatLvl";
    public static final String RESPONSE_PROTO = "myProtoDownload";

    private EventReporter reporter;

    private final String TAG = SyncingService.class.getSimpleName();

    // Constants
    private final int TIME_SYNC_OFFSET = 10000;
    public static final int MIN_SYNC_PAGES = 2;
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
                DeviceTransport driver = UsbSerialProber.acquire(
                        (UsbManager) getSystemService(USB_SERVICE));
                if (driver != null) {
                    handleActionSync(param1, getApplicationContext(), driver);
                }
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    protected void handleActionSync(int numOfPages, Context context, DeviceTransport serialDriver) {
        reporter = AndroidEventReporter.getReporter(context);
        boolean broadcastSent = false;
        AndroidPreferences preferences = new AndroidPreferences(context);
        Tracker tracker = ((Nightscout) context).getTracker();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
        wl.acquire();
        if (serialDriver != null) {
            AbstractUploaderDevice uploaderDevice = AndroidUploaderDevice.getUploaderDevice(context);
            AbstractDevice device = new DexcomG4(serialDriver, preferences, uploaderDevice);

            ((DexcomG4) device).setNumOfPages(numOfPages);
            ((CdcAcmSerialDriver) serialDriver).setPowerManagementEnabled(preferences.isRootEnabled());
            try {
                DownloadResults results = device.download();
                CookieMonsterDownload download = results.getDownload();

                Uploader uploader = new Uploader(context, preferences);
                boolean uploadStatus;
                if (numOfPages < 20) {
                    uploadStatus = uploader.upload(results, 1);
                } else {
                    uploadStatus = uploader.upload(results);
                }

                EGVRecord recentEGV = new EGVRecord(download.sgv.get(download.sgv.size() - 1));

                DateTime dt = new DateTime();
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                String iso8601Str = fmt.print(dt);

                CookieMonsterDownload.Builder builder = new CookieMonsterDownload.Builder();
                builder.download_timestamp(iso8601Str)
                        .download_status(download.download_status)
                        .receiver_battery(download.receiver_battery)
                        .uploader_battery(download.uploader_battery)
                        .units(download.units);

                long egvTime = preferences.getLastEgvMqttUpload();
                long meterTime = preferences.getLastMeterMqttUpload();
                long sensorTime = preferences.getLastSensorMqttUpload();
                long calTime = preferences.getLastCalMqttUpload();
                long lastRecord = 0;
                List<CookieMonsterG4SGV> filteredSgvs = new ArrayList<>();
                for (CookieMonsterG4SGV aRecord : download.sgv) {
                    if (aRecord.sys_timestamp_sec > egvTime) {
                        filteredSgvs.add(aRecord);
                        lastRecord = aRecord.sys_timestamp_sec;
                    }
                }
                builder.sgv(filteredSgvs);
                if (lastRecord != 0) {
                    preferences.setLastEgvMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Meter> filteredMeter = new ArrayList<>();
                for (CookieMonsterG4Meter aRecord : download.meter) {
                    if (aRecord.sys_timestamp_sec > meterTime) {
                        filteredMeter.add(aRecord);
                        lastRecord = aRecord.sys_timestamp_sec;
                    }
                }
                builder.meter(filteredMeter);
                // FIXME (klee) these values should be stored only after we are sure the message has been delivered.
                if (lastRecord != 0) {
                    preferences.setLastMeterMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Sensor> filteredSensor = new ArrayList<>();
                for (CookieMonsterG4Sensor aRecord : download.sensor) {
                    if (aRecord.sys_timestamp_sec > sensorTime) {
                        filteredSensor.add(aRecord);
                        lastRecord = aRecord.sys_timestamp_sec;
                    }
                }
                builder.sensor(filteredSensor);
                if (lastRecord != 0) {
                    preferences.setLastSensorMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Cal> filteredCal = new ArrayList<>();
                for (CookieMonsterG4Cal aRecord : download.cal) {
                    if (aRecord.sys_timestamp_sec > calTime) {
                        filteredCal.add(aRecord);
                        lastRecord = aRecord.sys_timestamp_sec;
                    }
                }
                builder.cal(filteredCal);
                if (lastRecord != 0) {
                    preferences.setLastCalMqttUpload(lastRecord);
                }
                broadcastSGVToUI(recentEGV, uploadStatus, results.getNextUploadTime() + TIME_SYNC_OFFSET,
                        results.getDisplayTime(), results.getResultArray(), download.receiver_battery,
                        builder.build().toByteArray());
                broadcastSent = true;
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

        if (!broadcastSent) broadcastSGVToUI();

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

    public void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus,
                                 long nextUploadTime, long displayTime,
                                 JSONArray json, int batLvl, byte[] proto) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, egvRecord.getBgMgdl());
        broadcastIntent.putExtra(RESPONSE_TREND, egvRecord.getTrend().getID());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().getTime());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, displayTime);
        if (proto != null) {
            broadcastIntent.putExtra(RESPONSE_PROTO, proto);
        }
        if (json != null)
            broadcastIntent.putExtra(RESPONSE_JSON, json.toString());
        broadcastIntent.putExtra(RESPONSE_BAT, batLvl);
        sendBroadcast(broadcastIntent);
    }

    protected void broadcastSGVToUI() {
        EGVRecord record = new EGVRecord(-1, TrendArrow.NONE, new Date(), new Date(), Noise.NOISE_NONE);
        broadcastSGVToUI(record, false, standardMinutes(5).getMillis() + TIME_SYNC_OFFSET, new Date().getTime(), null, 0, new byte[0]);
    }

}