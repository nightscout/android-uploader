package com.nightscout.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferencesValidator;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.GlucoseReading;
import com.nightscout.core.utils.RestUriUtils;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.nightscout.core.dexcom.SpecialValue.getEGVSpecialValue;
import static com.nightscout.core.dexcom.SpecialValue.isSpecialValue;
import static org.joda.time.Duration.standardMinutes;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_POLL = "com.nightscout.android.dexcom.action.POLL";

    // Receivers
    private CGMStatusReceiver mCGMStatusReceiver;

    private ToastReceiver toastReceiver;

    // Member components
    private Handler mHandler = new Handler();
    private Context mContext;
    private String mJSONData;
    private long lastRecordTime = -1;
    private long receiverOffsetFromUploader = 0;

    private NightscoutPreferences preferences;

    // Analytics mTracker
    private Tracker mTracker;

    // UI components
    private WebView mWebView;
    private TextView mTextSGV;
    private TextView mTextTimestamp;
    private StatusBarIcons statusBarIcons;
    private Pebble pebble;
    private AndroidUploaderDevice uploaderDevice;

    private AlarmManager alarmManager;
    private PendingIntent syncManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate called.");

        preferences = new AndroidPreferences(getApplicationContext());
        migrateToNewStyleRestUris();
        ensureSavedUrisAreValid();
        ensureIUnderstandDialogDisplayed();

        // Add timezone ID to ACRA report
        ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());

        mTracker = ((Nightscout) getApplicationContext()).getTracker();

        mContext = getApplicationContext();

        // Register USB attached/detached and battery changes intents
        IntentFilter deviceStatusFilter = new IntentFilter();
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        deviceStatusFilter.addAction(ACTION_POLL);
        registerReceiver(mDeviceStatusReceiver, deviceStatusFilter);

        // Register Broadcast Receiver for response messages from mSyncingServiceIntent service
        mCGMStatusReceiver = new CGMStatusReceiver();
        IntentFilter filter = new IntentFilter(CGMStatusReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mCGMStatusReceiver, filter);

        toastReceiver = new ToastReceiver();
        IntentFilter toastFilter = new IntentFilter(ToastReceiver.ACTION_SEND_NOTIFICATION);
        toastFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(toastReceiver, toastFilter);

        // Setup UI components
        setContentView(R.layout.activity_main);
        mTextSGV = (TextView) findViewById(R.id.sgValue);
        mTextSGV.setTag(R.string.display_sgv, -1);
        mTextSGV.setTag(R.string.display_trend, 0);
        mTextTimestamp = (TextView) findViewById(R.id.timeAgo);
        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(false);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setBackgroundColor(0);
        mWebView.loadUrl("file:///android_asset/index.html");
        statusBarIcons = new StatusBarIcons();

        // If app started due to android.hardware.usb.action.USB_DEVICE_ATTACHED intent, start syncing
        Intent startIntent = getIntent();
        String action = startIntent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                SyncingService.isG4Connected(getApplicationContext())) {
            statusBarIcons.setUSB(true);
            Log.d(TAG, "Starting syncing in OnCreate...");
            SyncingService.startActionSingleSync(mContext, SyncingService.MIN_SYNC_PAGES);
        } else {
            // reset the top icons to their default state
            statusBarIcons.setDefaults();
        }

        // Check (only once) to see if they have opted in to shared data for research
        if (!preferences.hasAskedForData()) {
            // Prompt user to ask to donate data to research
            AlertDialog.Builder dataDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.donate_dialog_title)
                    .setMessage(R.string.donate_dialog_summary)
                    .setPositiveButton(R.string.donate_dialog_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mTracker.send(new HitBuilders.EventBuilder("DataDonateQuery", "Yes").build());
                            preferences.setDataDonateEnabled(true);
                        }
                    })
                    .setNegativeButton(R.string.donate_dialog_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mTracker.send(new HitBuilders.EventBuilder("DataDonateQuery", "No").build());
                            preferences.setDataDonateEnabled(true);
                        }
                    })
                    .setIcon(R.drawable.ic_launcher);

            dataDialog.show();

            preferences.setAskedForData(true);
        }

        // Report API vs mongo stats once per session
        reportUploadMethods(mTracker);
        pebble = new Pebble(getApplicationContext());
        pebble.setUnits(preferences.getPreferredUnits());
        pebble.setPwdName(preferences.getPwdName());

        uploaderDevice = AndroidUploaderDevice.getUploaderDevice(getApplicationContext());
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent syncIntent = new Intent(MainActivity.ACTION_POLL);
        syncManager = PendingIntent.getBroadcast(getApplicationContext(), 1, syncIntent, 0);
    }

    public void reportUploadMethods(Tracker tracker) {
        if (preferences.isRestApiEnabled()) {
            for (String url : preferences.getRestApiBaseUris()) {
                String apiVersion = (RestUriUtils.isV1Uri(URI.create(url))) ? "WebAPIv1" : "Legacy WebAPI";
                tracker.send(new HitBuilders.EventBuilder("Upload", apiVersion).build());
            }
        }
        if (preferences.isMongoUploadEnabled()) {
            tracker.send(new HitBuilders.EventBuilder("Upload", "Mongo").build());
        }
    }

    private void migrateToNewStyleRestUris() {
        List<String> newUris = new ArrayList<>();
        for (String uriString : preferences.getRestApiBaseUris()) {
            if (uriString.contains("@http")) {
                List<String> splitUri = Splitter.on('@').splitToList(uriString);
                Uri oldUri = Uri.parse(splitUri.get(1));
                String newAuthority = Joiner.on('@').join(splitUri.get(0), oldUri.getEncodedAuthority());
                Uri newUri = oldUri.buildUpon().encodedAuthority(newAuthority).build();
                newUris.add(newUri.toString());
            } else {
                newUris.add(uriString);
            }
        }
        preferences.setRestApiBaseUris(newUris);
    }

    private void ensureSavedUrisAreValid() {
        if (PreferencesValidator.validateMongoUriSyntax(getApplicationContext(),
                preferences.getMongoClientUri()).isPresent()) {
            preferences.setMongoClientUri(null);
        }
        List<String> filteredRestUris = new ArrayList<>();
        for (String uri : preferences.getRestApiBaseUris()) {
            if (!PreferencesValidator.validateRestApiUriSyntax(getApplicationContext(), uri).isPresent()) {
                filteredRestUris.add(uri);
            }
        }
        preferences.setRestApiBaseUris(filteredRestUris);
    }

    private void ensureIUnderstandDialogDisplayed() {
        if (!preferences.getIUnderstand()) {
            // Prompt user to ask to donate data to research
            AlertDialog.Builder dataDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.pref_title_i_understand)
                    .setMessage(R.string.pref_summary_i_understand)
                    .setPositiveButton(R.string.donate_dialog_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.setIUnderstand(true);
                        }
                    })
                    .setNegativeButton(R.string.donate_dialog_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    })
                    .setIcon(R.drawable.ic_launcher);
            dataDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPaused called.");
        mWebView.pauseTimers();
        mWebView.onPause();
        mHandler.removeCallbacks(updateTimeAgo);
    }

    public void setPebble(Pebble pebble) {
        this.pebble = pebble;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResumed called.");
        mWebView.onResume();
        mWebView.resumeTimers();

        // Set and deal with mmol/L<->mg/dL conversions
        Log.d(TAG, "display_options_units: " + preferences.getPreferredUnits().name());
        pebble.config(preferences.getPwdName(), preferences.getPreferredUnits(), getApplicationContext());
        int sgv = (Integer) mTextSGV.getTag(R.string.display_sgv);

        int direction = (Integer) mTextSGV.getTag(R.string.display_trend);
        if (sgv != -1) {
            GlucoseReading sgvReading = new GlucoseReading(sgv, GlucoseUnit.MGDL);
            mTextSGV.setText(getSGVStringByUnit(sgvReading, TrendArrow.values()[direction]));
        }

        mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");

        mHandler.post(updateTimeAgo);
        // FIXME: (klee) need to find a better way to do this. Too many things are hooking in here.
        if (statusBarIcons != null) {
            statusBarIcons.checkForRootOptionChanged();
        }
    }

    private String getSGVStringByUnit(GlucoseReading sgv, TrendArrow trend) {
        String sgvStr = sgv.asStr(preferences.getPreferredUnits());
        return (sgv.asMgdl() != -1) ?
                (isSpecialValue(sgv)) ?
                        getEGVSpecialValue(sgv).get().toString() : sgvStr + " " + trend.symbol() : "---";
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        super.onDestroy();
        unregisterReceiver(mCGMStatusReceiver);
        unregisterReceiver(mDeviceStatusReceiver);
        unregisterReceiver(toastReceiver);
        if (pebble != null) {
            pebble.close();
        }
        if (uploaderDevice != null) {
            uploaderDevice.close();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        outState.putString("saveJSONData", mJSONData);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
        outState.putBoolean("saveImageViewUSB", statusBarIcons.getUSB());
        outState.putBoolean("saveImageViewUpload", statusBarIcons.getUpload());
        outState.putBoolean("saveImageViewTimeIndicator", statusBarIcons.getTimeIndicator());
        outState.putInt("saveImageViewBatteryIndicator", statusBarIcons.getBatteryIndicator());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
        mJSONData = savedInstanceState.getString("mJSONData");
        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
        statusBarIcons.setUSB(savedInstanceState.getBoolean("saveImageViewUSB"));
        statusBarIcons.setUpload(savedInstanceState.getBoolean("saveImageViewUpload"));
        statusBarIcons.setTimeIndicator(savedInstanceState.getBoolean("saveImageViewTimeIndicator"));
        statusBarIcons.setBatteryIndicator(savedInstanceState.getInt("saveImageViewBatteryIndicator"));
    }

    public class CGMStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_RESPONSE = "com.mSyncingServiceIntent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get response messages from broadcast
            int responseSGV = intent.getIntExtra(SyncingService.RESPONSE_SGV, -1);
            GlucoseReading reading = new GlucoseReading(responseSGV, GlucoseUnit.MGDL);
            TrendArrow trend = TrendArrow.values()[intent.getIntExtra(SyncingService.RESPONSE_TREND, 0)];
            long responseSGVTimestamp = intent.getLongExtra(SyncingService.RESPONSE_TIMESTAMP, -1L);
            boolean responseUploadStatus = intent.getBooleanExtra(SyncingService.RESPONSE_UPLOAD_STATUS, false);
            long responseNextUploadTime = intent.getLongExtra(SyncingService.RESPONSE_NEXT_UPLOAD_TIME, -1);
            long responseDisplayTime = intent.getLongExtra(SyncingService.RESPONSE_DISPLAY_TIME, new Date().getTime());
            lastRecordTime = responseSGVTimestamp;
            receiverOffsetFromUploader = new Date().getTime() - responseDisplayTime;
            int rcvrBat = intent.getIntExtra(SyncingService.RESPONSE_BAT, -1);
            String json = intent.getStringExtra(SyncingService.RESPONSE_JSON);

            if (responseSGV != -1) {
                pebble.sendDownload(reading, trend, responseSGVTimestamp, getApplicationContext());
            }
            // Reload d3 chart with new data
            if (json != null) {
                mJSONData = json;
                mWebView.loadUrl("javascript:updateData(" + mJSONData + ")");
            }

            // Update icons
            statusBarIcons.setUpload(responseUploadStatus);

            // Update UI with latest record information
            mTextSGV.setText(getSGVStringByUnit(reading, trend));
            mTextSGV.setTag(R.string.display_sgv, reading.asMgdl());
            mTextSGV.setTag(R.string.display_trend, trend.ordinal());

            String timeAgoStr = "---";
            Log.d(TAG, "Date: " + new Date().getTime());
            Log.d(TAG, "Response SGV Timestamp: " + responseSGVTimestamp);
            if (responseSGVTimestamp > 0) {
                timeAgoStr = Utils.getTimeString(new Date().getTime() - responseSGVTimestamp);
            }

            mTextTimestamp.setText(timeAgoStr);
            mTextTimestamp.setTag(timeAgoStr);

            long nextUploadTime = standardMinutes(5).getMillis();

            if (responseNextUploadTime > nextUploadTime) {
                Log.d(TAG, "Receiver's time is less than current record time, possible time change.");
                mTracker.send(new HitBuilders.EventBuilder("Main", "Time change").build());
            } else if (responseNextUploadTime > 0) {
                Log.d(TAG, "Setting next upload time to: " + responseNextUploadTime);
                nextUploadTime = responseNextUploadTime;
            } else {
                Log.d(TAG, "OUT OF RANGE: Setting next upload time to: " + nextUploadTime + " ms.");
            }

            if (Minutes.minutesBetween(new DateTime(), new DateTime(responseDisplayTime))
                    .isGreaterThan(Minutes.minutes(20))) {
                Log.w(TAG, "Receiver time is off by 20 minutes or more.");
                mTracker.send(new HitBuilders.EventBuilder("Main", "Time difference > 20 minutes").build());
                statusBarIcons.setTimeIndicator(false);
            } else {
                statusBarIcons.setTimeIndicator(true);
            }

            statusBarIcons.setBatteryIndicator(rcvrBat);

            setNextPoll(nextUploadTime);
        }
    }

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    statusBarIcons.setDefaults();
                    cancelPoll();
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    statusBarIcons.setUSB(true);
                    Log.d(TAG, "Starting syncing on USB attached...");
                    SyncingService.startActionSingleSync(mContext, SyncingService.MIN_SYNC_PAGES);
                    break;
                case MainActivity.ACTION_POLL:
                    SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.MIN_SYNC_PAGES);
            }
        }
    };

    // Runnable to start service as needed to sync with mCGMStatusReceiver and cloud
    public Runnable syncCGM = new Runnable() {
        public void run() {
            SyncingService.startActionSingleSync(mContext, SyncingService.MIN_SYNC_PAGES);
        }
    };

    //FIXME: Strongly suggest refactoring this
    public Runnable updateTimeAgo = new Runnable() {
        @Override
        public void run() {
            long delta = new Date().getTime() - lastRecordTime + receiverOffsetFromUploader;
            if (lastRecordTime == 0) delta = 0;
            String timeAgoStr = "";
            if (lastRecordTime == -1) {
                timeAgoStr = "---";
            } else if (delta < 0) {
                timeAgoStr = getString(R.string.TimeChangeDetected);
            } else {
                timeAgoStr = Utils.getTimeString(delta);
            }
            mTextTimestamp.setText(timeAgoStr);
            mHandler.removeCallbacks(updateTimeAgo);
            mHandler.postDelayed(updateTimeAgo, standardMinutes(1).getMillis());
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.feedback_settings) {
            ACRAConfiguration acraConfiguration = ACRA.getConfig();
            // Set to dialog to get user comments
            try {
                acraConfiguration.setMode(ReportingInteractionMode.DIALOG);
                acraConfiguration.setResToastText(0);
            } catch (ACRAConfigurationException e) {
                e.printStackTrace();
            }
            ACRA.getErrorReporter().handleException(null);
            // Reset back to toast
            try {
                acraConfiguration.setResToastText(R.string.crash_toast_text);
                acraConfiguration.setMode(ReportingInteractionMode.TOAST);
            } catch (ACRAConfigurationException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.gap_sync) {
            SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.GAP_SYNC_PAGES);
        } else if (id == R.id.close_settings) {
            cancelPoll();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public class StatusBarIcons {
        private ImageView mImageViewUSB;
        private ImageView mImageViewUpload;
        private ImageView mImageViewTimeIndicator;
        private ImageView mImageRcvrBattery;
        private TextView mRcvrBatteryLabel;
        private boolean usbActive;
        private boolean uploadActive;
        private boolean displayTimeSync;
        private int batteryLevel;

        StatusBarIcons() {
            mImageViewUSB = (ImageView) findViewById(R.id.imageViewUSB);
            mImageViewUpload = (ImageView) findViewById(R.id.imageViewUploadStatus);
            mImageViewTimeIndicator = (ImageView) findViewById(R.id.imageViewTimeIndicator);

            mImageRcvrBattery = (ImageView) findViewById(R.id.imageViewRcvrBattery);
            mImageRcvrBattery.setImageResource(R.drawable.battery);
            mRcvrBatteryLabel = (TextView) findViewById(R.id.rcvrBatteryLabel);

            setDefaults();
        }

        public void checkForRootOptionChanged() {
            if (((AndroidPreferences) preferences).isRootEnabled()) {
                mImageRcvrBattery.setVisibility(View.GONE);
                mRcvrBatteryLabel.setVisibility(View.GONE);
            } else {
                mImageRcvrBattery.setVisibility(View.VISIBLE);
                mRcvrBatteryLabel.setVisibility(View.VISIBLE);
            }
        }


        public void setDefaults() {
            setUSB(false);
            setUpload(false);
            setTimeIndicator(false);
            setBatteryIndicator(0);
        }

        public void setUSB(boolean active) {
            usbActive = active;
            if (active) {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_connected);
                mImageViewUSB.setTag(R.drawable.ic_usb_connected);
            } else {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_disconnected);
                mImageViewUSB.setTag(R.drawable.ic_usb_disconnected);
            }
        }

        public void setUpload(boolean active) {
            uploadActive = active;
            if (active) {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_success);
                mImageViewUpload.setTag(R.drawable.ic_upload_success);
            } else {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_fail);
                mImageViewUpload.setTag(R.drawable.ic_upload_fail);
            }
        }

        public void setTimeIndicator(boolean active) {
            displayTimeSync = active;
            if (active) {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_good);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_good);
            } else {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_bad);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_bad);
            }
        }

        public void setBatteryIndicator(int batLvl) {
            batteryLevel = batLvl;
            mImageRcvrBattery.setImageLevel(batteryLevel);
            mImageRcvrBattery.setTag(batteryLevel);
        }

        public boolean getUSB() {
            return usbActive;
        }

        public boolean getUpload() {
            return uploadActive;
        }

        public boolean getTimeIndicator() {
            return displayTimeSync;
        }

        public int getBatteryIndicator() {
            if (mImageRcvrBattery == null) {
                return 0;
            }
            return (Integer) mImageRcvrBattery.getTag();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setNextPoll(long millis) {
        Log.d(TAG, "Setting next poll with Alarm for " + (millis) + " ms from now");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
        }
    }

    public void cancelPoll() {
        Log.d(TAG, "Canceling next alarm poll");
        alarmManager.cancel(syncManager);
    }
}