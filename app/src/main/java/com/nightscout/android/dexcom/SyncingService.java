package com.nightscout.android.dexcom;

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
import com.google.common.collect.Lists;
import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.R;
import com.nightscout.android.USB.USBPower;
import com.nightscout.android.USB.UsbSerialDriver;
import com.nightscout.android.USB.UsbSerialProber;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.upload.Uploader;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.protobuf.CookieMonsterG4Cal;
import com.nightscout.core.protobuf.CookieMonsterG4Download;
import com.nightscout.core.protobuf.CookieMonsterG4Meter;
import com.nightscout.core.protobuf.CookieMonsterG4SGV;
import com.nightscout.core.protobuf.CookieMonsterG4Sensor;
import com.nightscout.core.protobuf.DownloadStatus;
import com.nightscout.core.protobuf.GlucoseUnit;
import com.nightscout.core.protobuf.Noise;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.joda.time.Duration.standardMinutes;
import static org.joda.time.Duration.standardSeconds;

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
                UsbSerialDriver driver = acquireSerialDevice();
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
    protected void handleActionSync(int numOfPages, Context context, UsbSerialDriver serialDriver) {
        reporter = AndroidEventReporter.getReporter(context);
        boolean broadcastSent = false;
        AndroidPreferences preferences = new AndroidPreferences(context);
        Tracker tracker = ((Nightscout) context).getTracker();

        if (preferences.isRootEnabled()) USBPower.PowerOn();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
        wl.acquire();
        if (serialDriver != null) {
            try {
                ReadData readData = new ReadData(serialDriver);
                // TODO: need to check if numOfPages if valid on ReadData side
                EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
                List<MeterRecord> meterRecords = Lists.newArrayList(readData.getRecentMeterRecords());
                // TODO: need to check if numOfPages if valid on ReadData side
                SensorRecord[] sensorRecords = readData.getRecentSensorRecords(numOfPages);
                GlucoseDataSet[] glucoseDataSets = Utils.mergeGlucoseDataRecords(recentRecords, sensorRecords);

                CalRecord[] calRecords = new CalRecord[1];
                if (preferences.isCalibrationUploadEnabled()) {
                    calRecords = readData.getRecentCalRecords();
                }
                List<GlucoseDataSet> glucoseDataSetsList = Lists.newArrayList(glucoseDataSets);
                List<CalRecord> calRecordsList = Lists.newArrayList(calRecords);

                long timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
                reporter.report(EventType.DEVICE, EventSeverity.INFO,
                        context.getString(R.string.event_sync_log));
                // TODO: determine if the logic here is correct. I suspect it assumes the last record was less than 5
                // minutes ago. If a reading is skipped and the device is plugged in then nextUploadTime will be
                // set to a negative number. This situation will eventually correct itself.
                long nextUploadTime = standardMinutes(5).minus(standardSeconds(timeSinceLastRecord)).getMillis();
                long displayTime = readData.readDisplayTime().getTime();
                // FIXME: readData.readBatteryLevel() seems to flake out on battery level reads. Removing for now.
                int batLevel = 100;

                // convert into json for d3 plot
                JSONArray array = new JSONArray();
                for (EGVRecord recentRecord : recentRecords) {
                    array.put(recentRecord.toJSON());
                }


                Uploader uploader = new Uploader(context, preferences);
                // TODO: This should be cleaned up, 5 should be a constant, maybe handle in uploader,
                // and maybe might not have to read 5 pages (that was only done for single sync for UI
                // plot updating and might be able to be done in javascript d3 code as a FIFO array
                // Only upload 1 record unless forcing a sync
                boolean uploadStatus;
                if (numOfPages < 20) {
                    uploadStatus = uploader.upload(glucoseDataSetsList.get(glucoseDataSetsList.size() - 1),
                            meterRecords.get(meterRecords.size() - 1),
                            calRecordsList.get(calRecordsList.size() - 1));
                } else {
                    uploadStatus = uploader.upload(glucoseDataSetsList, meterRecords, calRecordsList);
                }

                EGVRecord recentEGV = recentRecords[recentRecords.length - 1];

                DateTime dt = new DateTime();
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                String iso8601Str = fmt.print(dt);

                CookieMonsterG4Download.Builder builder = new CookieMonsterG4Download.Builder();
                builder.download_timestamp(iso8601Str)
                        .download_status(DownloadStatus.SUCCESS)
                        .receiver_battery(batLevel)
                        .uploader_battery(MainActivity.batLevel)
                        .units(GlucoseUnit.MGDL);

                long egvTime = preferences.getLastEgvMqttUpload();
                long meterTime = preferences.getLastMeterMqttUpload();
                long sensorTime = preferences.getLastSensorMqttUpload();
                long calTime = preferences.getLastCalMqttUpload();
                long lastRecord = 0;
                List<CookieMonsterG4SGV> sgvList = Lists.newArrayList();
                for (EGVRecord aRecord : recentRecords) {
                    if (aRecord.getSystemTimeSeconds() > egvTime) {
                        sgvList.add(aRecord.toProtobuf());
                        lastRecord = aRecord.getSystemTimeSeconds();
                    }
                }
                builder.sgv(sgvList);
                if (lastRecord != 0) {
                    preferences.setLastEgvMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Meter> meterRecordList = Lists.newArrayList();
                for (MeterRecord aRecord : meterRecords) {
                    if (aRecord.getSystemTimeSeconds() > meterTime) {
                        meterRecordList.add(aRecord.toProtobuf());
                        lastRecord = aRecord.getSystemTimeSeconds();
                    }
                }
                builder.meter(meterRecordList);
                // FIXME (klee) these values should be stored only after we are sure the message has been delivered.
                if (lastRecord != 0) {
                    preferences.setLastMeterMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Sensor> sensorList = Lists.newArrayList();
                for (SensorRecord aRecord : sensorRecords) {
                    if (aRecord.getSystemTimeSeconds() > sensorTime) {
                        sensorList.add(aRecord.toProtobuf());
                        lastRecord = aRecord.getSystemTimeSeconds();
                    }
                }
                builder.sensor(sensorList);
                if (lastRecord != 0) {
                    preferences.setLastSensorMqttUpload(lastRecord);
                }
                lastRecord = 0;
                List<CookieMonsterG4Cal> calList = Lists.newArrayList();
                for (CalRecord aRecord : calRecordsList) {
                    if (aRecord == null) {
                        break;
                    }
                    if (aRecord.getSystemTimeSeconds() > calTime) {
                        calList.add(aRecord.toProtobuf());
                        lastRecord = aRecord.getSystemTimeSeconds();
                    }
                }
                builder.cal(calList);
                if (lastRecord != 0) {
                    preferences.setLastCalMqttUpload(lastRecord);
                }
                broadcastSGVToUI(recentEGV, uploadStatus, nextUploadTime + TIME_SYNC_OFFSET,
                        displayTime, array, batLevel, builder.build().toByteArray());
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
            } finally {
                // Close serial
                try {
                    serialDriver.close();
                } catch (IOException e) {
                    tracker.send(new HitBuilders.ExceptionBuilder()
                                    .setDescription("Unable to close serial connection")
                                    .setFatal(false)
                                    .build()
                    );
                    Log.e(TAG, "Unable to close", e);
                }

                // Try powering off, will only work if rooted
                if (preferences.isRootEnabled()) USBPower.PowerOff();
            }
        }

        if (!broadcastSent) broadcastSGVToUI();

        wl.release();
    }

    protected UsbSerialDriver acquireSerialDevice() {
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbSerialDriver serialDevice = UsbSerialProber.acquire(mUsbManager);
        if (serialDevice != null) {
            try {
                serialDevice.open();
                return serialDevice;
            } catch (IOException e) {
                Log.e(TAG, "Unable to open USB. ", e);
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
        return serialDevice;
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
        broadcastSGVToUI(record, false, standardMinutes(5).getMillis() + TIME_SYNC_OFFSET, new Date().getTime(), null, 0, null);
    }

}