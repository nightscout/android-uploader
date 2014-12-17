package com.nightscout.android.dexcom;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.collect.Lists;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.R;
import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.upload.Uploader;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.dexcom.NoiseMode;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.SensorRecord;
import com.nightscout.core.preferences.NightscoutPreferences;

import org.json.JSONArray;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.joda.time.Duration.standardMinutes;
import static org.joda.time.Duration.standardSeconds;

/**
 * An {@link IntentService} subclass for handling asynchronous CGM Receiver downloads and cloud
 * uploads requests in a service on a separate handler thread.
 */
public class SyncingService extends IntentService {

  // Response to broadcast to activity
  public static final String RESPONSE_SGV = "mySGV";
  public static final String RESPONSE_TREND = "myTrend";
  public static final String RESPONSE_TIMESTAMP = "myTimestamp";
  public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";
  public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
  public static final String RESPONSE_DISPLAY_TIME = "myDisplayTime";
  public static final String RESPONSE_JSON = "myJSON";
  public static final String RESPONSE_BAT = "myBatLvl";
  public static final int MIN_SYNC_PAGES = 2;
  public static final int GAP_SYNC_PAGES = 20;
  // Action for intent
  private static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";
  // Parameters for intent
  private static final String SYNC_PERIOD = "com.nightscout.android.dexcom.extra.SYNC_PERIOD";
  private final String TAG = SyncingService.class.getSimpleName();
  // Constants
  private final int TIME_SYNC_OFFSET = 10000;
  private Context mContext;
  private UsbManager mUsbManager;
  private UsbSerialDriver mSerialDevice;


  public SyncingService() {
    super("SyncingService");
  }

  /**
   * Starts this service to perform action Single Sync with the given parameters. If the service is
   * already performing a task this action will be queued.
   *
   * @see IntentService
   */
  public static void startActionSingleSync(Context context, int numOfPages) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    // Exit if the user hasn't selected "I understand"
    if (!prefs.getBoolean("i_understand", false)) {
      Toast.makeText(context, R.string.message_user_not_understand, Toast.LENGTH_LONG).show();
      return;
    }

    Intent intent = new Intent(context, SyncingService.class);
    intent.setAction(ACTION_SYNC);
    intent.putExtra(SYNC_PERIOD, numOfPages);
    context.startService(intent);
  }

  static public boolean isG4Connected(Context c) {
    // Iterate through devices and see if the dexcom is connected
    // Allowing us to start to start syncing if the G4 is already connected
    // vendor-id="8867" product-id="71" class="2" subclass="0" protocol="0"
    UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
    if (manager == null) {
      return false;
    }
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
   * Handle action Sync in the provided background thread with the provided parameters.
   */
  private void handleActionSync(int numOfPages) {
    boolean broadcastSent = false;
    boolean
        rootEnabled =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .getBoolean("root_support_enabled", false);
    Tracker tracker = ((Nightscout) getApplicationContext()).getTracker();

    if (rootEnabled) {
      USBPower.PowerOn();
    }

    PowerManager
        pm =
        (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
    wl.acquire();

    if (acquireSerialDevice()) {
      try {
        ReadData readData = new ReadData(mSerialDevice);
        // TODO: need to check if numOfPages if valid on ReadData side
        EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
        List<MeterRecord> meterRecords = Lists.newArrayList(readData.getRecentMeterRecords());
        // TODO: need to check if numOfPages if valid on ReadData side
        SensorRecord[] sensorRecords = readData.getRecentSensorRecords(numOfPages);
        GlucoseDataSet[]
            glucoseDataSets =
            Utils.mergeGlucoseDataRecords(recentRecords, sensorRecords);

        // FIXME: This is a workaround for the new Dexcom AP which seems to have a new format
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        CalRecord[] calRecords = new CalRecord[1];
        if (prefs.getBoolean("cloud_cal_data", false)) {
          calRecords = readData.getRecentCalRecords();
        }
        List<GlucoseDataSet> glucoseDataSetsList = Lists.newArrayList(glucoseDataSets);
        List<CalRecord> calRecordsList = Lists.newArrayList(calRecords);

        long
            timeSinceLastRecord =
            readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
        // TODO: determine if the logic here is correct. I suspect it assumes the last record was less than 5
        // minutes ago. If a reading is skipped and the device is plugged in then nextUploadTime will be
        // set to a negative number. This situation will eventually correct itself.
        long
            nextUploadTime =
            standardMinutes(5).minus(standardSeconds(timeSinceLastRecord)).getMillis();
        long displayTime = readData.readDisplayTime().getTime();
        // FIXME: readData.readBatteryLevel() seems to flake out on battery level reads. Removing for now.
        int batLevel = 100;

        // convert into json for d3 plot
        JSONArray array = new JSONArray();
        for (int i = 0; i < recentRecords.length; i++) {
          array.put(recentRecords[i].toJSON());
        }

        NightscoutPreferences preferences = new AndroidPreferences(getApplicationContext());
        Uploader uploader = new Uploader(mContext, preferences);
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
        broadcastSGVToUI(recentEGV, uploadStatus, nextUploadTime + TIME_SYNC_OFFSET,
                         displayTime, array, batLevel);
        broadcastSent = true;
      } catch (ArrayIndexOutOfBoundsException e) {
        Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
        tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Array Index out of bounds")
                         .setFatal(false)
                         .build());
      } catch (NegativeArraySizeException e) {
        Log.wtf("Negative array exception from receiver", e);
        tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Negative Array size")
                         .setFatal(false)
                         .build());
      } catch (IndexOutOfBoundsException e) {
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
        Log.wtf("CRC failed", e);
        tracker.send(new HitBuilders.ExceptionBuilder().setDescription("CRC Failed")
                         .setFatal(false)
                         .build());
      } catch (Exception e) {
        Log.wtf("Unhandled exception caught", e);
        tracker.send(new HitBuilders.ExceptionBuilder()
                         .setDescription("Catch all exception in handleActionSync")
                         .setFatal(false)
                         .build());
      } finally {
        // Close serial
        try {
          mSerialDevice.close();
        } catch (IOException e) {
          tracker.send(new HitBuilders.ExceptionBuilder()
                           .setDescription("Unable to close serial connection")
                           .setFatal(false)
                           .build()
          );
          Log.e(TAG, "Unable to close", e);
        }

        // Try powering off, will only work if rooted
        if (rootEnabled) {
          USBPower.PowerOff();
        }
      }
    }

    if (!broadcastSent) {
      broadcastSGVToUI();
    }

    wl.release();
  }

  private boolean acquireSerialDevice() {
    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    mSerialDevice = UsbSerialProber.acquire(mUsbManager);
    if (mSerialDevice != null) {
      try {
        mSerialDevice.open();
        return true;
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
    return false;
  }

  private void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus,
                                long nextUploadTime, long displayTime,
                                JSONArray json, int batLvl) {
    Log.d(TAG, "Current EGV: " + egvRecord.getBGValue());
    Intent broadcastIntent = new Intent();
    broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    broadcastIntent.putExtra(RESPONSE_SGV, egvRecord.getBGValue());
    broadcastIntent.putExtra(RESPONSE_TREND, egvRecord.getTrend().getID());
    broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().getTime());
    broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
    broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
    broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, displayTime);
    if (json != null) {
      broadcastIntent.putExtra(RESPONSE_JSON, json.toString());
    }
    broadcastIntent.putExtra(RESPONSE_BAT, batLvl);
    sendBroadcast(broadcastIntent);
  }

  private void broadcastSGVToUI() {
    EGVRecord record = new EGVRecord(-1, TrendArrow.NONE, new Date(), new Date(), NoiseMode.None);
    broadcastSGVToUI(record, false, standardMinutes(5).getMillis() + TIME_SYNC_OFFSET,
                     new Date().getTime(), null, 0);
  }

}