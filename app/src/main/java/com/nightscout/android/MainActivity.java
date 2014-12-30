package com.nightscout.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.nightscout.android.drivers.AndroidUploaderDevice;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.events.UserEventPanelActivity;
import com.nightscout.android.mqtt.AndroidMqttPinger;
import com.nightscout.android.mqtt.AndroidMqttTimer;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferencesValidator;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.wearables.Pebble;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.mqtt.MqttEventMgr;
import com.nightscout.core.mqtt.MqttPinger;
import com.nightscout.core.mqtt.MqttTimer;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.GlucoseReading;
import com.nightscout.core.utils.RestUriUtils;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.nightscout.core.dexcom.SpecialValue.getEGVSpecialValue;
import static com.nightscout.core.dexcom.SpecialValue.isSpecialValue;
import static org.joda.time.Duration.standardMinutes;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

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
    private ImageButton mSyncButton;
    private ImageButton mUsbButton;
    private Pebble pebble;
    private AndroidUploaderDevice uploaderDevice;
    private MqttEventMgr mqttManager;
    private AndroidEventReporter reporter;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate called.");

        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        reporter.report(EventType.APPLICATION, EventSeverity.INFO,
                getApplicationContext().getString(R.string.app_started));

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
        mUsbButton = (ImageButton) findViewById(R.id.usbButton);
        mSyncButton = (ImageButton) findViewById(R.id.syncButton);
        mUsbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.DEVICE.ordinal());
                startActivity(intent);
            }
        });
        mSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
                startActivity(intent);
            }
        });


        // If app started due to android.hardware.usb.action.USB_DEVICE_ATTACHED intent, start syncing
        Intent startIntent = getIntent();
        String action = startIntent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                SyncingService.isG4Connected(getApplicationContext())) {
            reporter.report(EventType.DEVICE, EventSeverity.INFO,
                    getApplicationContext().getString(R.string.g4_connected));
            mUsbButton.setBackgroundResource(R.drawable.ic_usb);
            Log.d(TAG, "Starting syncing in OnCreate...");
            SyncingService.startActionSingleSync(mContext, SyncingService.MIN_SYNC_PAGES);
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

        try {
            setupMqtt();
            mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
        } catch (MqttException e) {
            mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
        }
    }

    public void setupMqtt() throws MqttException {
        if (preferences.isMqttEnabled()) {
            if (!preferences.getMqttUser().equals("") && !preferences.getMqttPass().equals("") &&
                    !preferences.getMqttEndpoint().equals("")) {
                MqttConnectOptions mqttOptions = new MqttConnectOptions();
                mqttOptions.setCleanSession(true);
                mqttOptions.setKeepAliveInterval(150000);
                mqttOptions.setUserName(preferences.getMqttUser());
                mqttOptions.setPassword(preferences.getMqttPass().toCharArray());
                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                MemoryPersistence dataStore = new MemoryPersistence();
                MqttClient client = new MqttClient(preferences.getMqttEndpoint(), androidId, dataStore);
                MqttPinger pinger = new AndroidMqttPinger(getApplicationContext(), 0, client, 150000);
                MqttTimer timer = new AndroidMqttTimer(getApplicationContext(), 0);
                mqttManager = new MqttEventMgr(client, mqttOptions, pinger, timer, reporter);
                mqttManager.connect();
                mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
            }
        }
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
        List<String> newUris = Lists.newArrayList();
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
        List<String> filteredRestUris = Lists.newArrayList();
        for (String uri : preferences.getRestApiBaseUris()) {
            if (!PreferencesValidator.validateRestApiUriSyntax(getApplicationContext(), uri).isPresent()) {
                filteredRestUris.add(uri);
            }
        }
        preferences.setRestApiBaseUris(filteredRestUris);
        if (PreferencesValidator.validateMqttEndpointSyntax(getApplicationContext(),
                preferences.getMqttEndpoint()).isPresent()) {
            preferences.setMqttEndpoint(null);
        }
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
        pebble.config(preferences.getPwdName(), preferences.getPreferredUnits());
        int sgv = (Integer) mTextSGV.getTag(R.string.display_sgv);

        int direction = (Integer) mTextSGV.getTag(R.string.display_trend);
        if (sgv != -1) {
            GlucoseReading sgvReading = new GlucoseReading(sgv, GlucoseUnit.MGDL);
            mTextSGV.setText(getSGVStringByUnit(sgvReading, TrendArrow.values()[direction]));
        }

        mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");

        mHandler.post(updateTimeAgo);
        try {
            if (mqttManager != null && mqttManager.isConnected()) {
                MqttClient client = mqttManager.getClient();
                MqttConnectOptions options = mqttManager.getOptions();
                boolean mqttOptsChanged = !preferences.getMqttEndpoint().equals(client.getServerURI());
                mqttOptsChanged &= !preferences.getMqttUser().equals(options.getUserName());
                mqttOptsChanged &= !preferences.getMqttPass().equals(String.valueOf(options.getPassword()));
                if (mqttOptsChanged) {
                    mqttManager.disconnect();
                    setupMqtt();
                }
            }

            if ((mqttManager == null || !mqttManager.isConnected()) && preferences.isMqttEnabled()) {
                setupMqtt();
            }

            if (mqttManager != null && mqttManager.isConnected() && !preferences.isMqttEnabled()) {
                mqttManager.close();
            }
        } catch (MqttException e) {
            mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
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
        if (mqttManager != null) {
            mqttManager.close();
        }
        uploaderDevice.close();
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
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
        mJSONData = savedInstanceState.getString("mJSONData");
        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
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
            byte[] proto = intent.getByteArrayExtra(SyncingService.RESPONSE_PROTO);
            boolean published = false;
            if (proto != null && !Arrays.equals(proto, new byte[0])) {
                Log.d(TAG, "Proto: " + Utils.bytesToHex(proto));
                if (mqttManager != null) {
                    Log.d(TAG, "Publishing");
                    mqttManager.publish(proto, "/downloads/protobuf");
                    published = true;
                } else {
                    Log.e(TAG, "Not publishing for some reason");
                }
            }
            if (preferences.isMqttEnabled()) {
                responseUploadStatus &= published;
            }
            if (responseUploadStatus) {
                mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
            } else {
                mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
            }
            if (responseSGV != -1) {
                pebble.sendDownload(reading, trend, responseSGVTimestamp);
            }
            // Reload d3 chart with new data
            if (json != null) {
                mJSONData = json;
                mWebView.loadUrl("javascript:updateData(" + mJSONData + ")");
            }

            // Update UI with latest record information
            mTextSGV.setText(getSGVStringByUnit(reading, trend));
            mTextSGV.setTag(R.string.display_sgv, reading.asMgdl());
            mTextSGV.setTag(R.string.display_trend, trend.getID());

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
            }

            mHandler.removeCallbacks(syncCGM);
            mHandler.postDelayed(syncCGM, nextUploadTime);
            // Start updating the timeago only if the screen is on
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn())
                mHandler.postDelayed(updateTimeAgo, nextUploadTime / 5);
        }
    }

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    reporter.report(EventType.DEVICE, EventSeverity.INFO,
                            getApplicationContext().getString(R.string.g4_disconnected));
//                    statusBarIcons.setDefaults();
                    mUsbButton.setBackgroundResource(R.drawable.ic_nousb);
                    mHandler.removeCallbacks(syncCGM);
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    reporter.report(EventType.DEVICE, EventSeverity.INFO,
                            getApplicationContext().getString(R.string.g4_connected));
                    mUsbButton.setBackgroundResource(R.drawable.ic_usb);
//                    statusBarIcons.setUSB(true);
                    Log.d(TAG, "Starting syncing on USB attached...");
                    SyncingService.startActionSingleSync(mContext, SyncingService.MIN_SYNC_PAGES);
                    break;
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.feedback_settings:
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
                break;
            case R.id.gap_sync:
                SyncingService.startActionSingleSync(getApplicationContext(),
                        SyncingService.GAP_SYNC_PAGES);
                break;
            case R.id.event_log:
                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.ALL.ordinal());
                startActivity(intent);
                break;
            case R.id.usb_log:
                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.DEVICE.ordinal());
                startActivity(intent);
                break;
            case R.id.upload_log:
                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
                startActivity(intent);
                break;
            case R.id.close_settings:
                mHandler.removeCallbacks(syncCGM);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}