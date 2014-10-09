package com.nightscout.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.dexcom.Constants;
import com.nightscout.android.dexcom.SyncingService;
import com.nightscout.android.dexcom.Utils;
import com.nightscout.android.settings.SettingsActivity;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();

    // Receivers
    private CGMStatusReceiver mCGMStatusReceiver;

    // Member components
    private Handler mHandler = new Handler();
    private Context mContext;
    private String mJSONData;
    private long lastRecordTime = -1;

    // Analytics mTracker
    Tracker mTracker;

    // UI components
    private WebView mWebView;
    private TextView mTextSGV;
    private TextView mTextTimestamp;
    StatusBarIcons statusBarIcons;

    // TODO: should try and avoid use static
    public static int batLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add timezone ID to ACRA report
        ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());

        mTracker = ((Nightscout) getApplicationContext()).getTracker();

        mContext = getApplicationContext();

        // Register USB attached/detached and battery changes intents
        IntentFilter deviceStatusFilter = new IntentFilter();
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        deviceStatusFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mDeviceStatusReceiver, deviceStatusFilter);

        // Register Broadcast Receiver for response messages from mSyncingServiceIntent service
        mCGMStatusReceiver = new CGMStatusReceiver();
        IntentFilter filter = new IntentFilter(CGMStatusReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mCGMStatusReceiver, filter);

        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStateReceiver,screenFilter);

        // Setup UI components
        setContentView(R.layout.activity_main);
        mTextSGV = (TextView) findViewById(R.id.sgValue);
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
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || SyncingService.isG4Connected(getApplicationContext())) {
            statusBarIcons.setUSB(true);
            Log.d(TAG, "Starting syncing in OnCreate...");
            // TODO: 2nd parameter should be static constant from intent service
            SyncingService.startActionSingleSync(mContext, 5);
        } else {
            // reset the top icons to their default state
            statusBarIcons.setDefaults();
        }

        // Report API vs mongo stats once per session
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (prefs.getBoolean("cloud_storage_api_enable", false)) {
            String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
            ArrayList<String> baseURIs = new ArrayList<String>();
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
                String apiVersion;
                apiVersion=(baseURL.endsWith("/v1/"))?"WebAPIv1":"Legacy WebAPI";
                mTracker.send(new HitBuilders.EventBuilder("Upload", apiVersion).build());
            }
        }
        if (prefs.getBoolean("cloud_storage_mongodb_enable", false)) {
            mTracker.send(new HitBuilders.EventBuilder("Upload", "Mongo").build());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPaused called.");
        mWebView.pauseTimers();
        mWebView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResumed called.");
        mWebView.onResume();
        mWebView.resumeTimers();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        super.onDestroy();
        unregisterReceiver(mCGMStatusReceiver);
        unregisterReceiver(mDeviceStatusReceiver);
        unregisterReceiver(screenStateReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        outState.putString("saveJSONData", mJSONData);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
        outState.putBoolean("saveImageViewUSB", statusBarIcons.getUSB());
        outState.putBoolean("saveImageViewUpload", statusBarIcons.getUpload());
        outState.putBoolean("saveImageViewTimeIndicator", statusBarIcons.getTimeIndicator());
        outState.putInt("saveImageViewBatteryIndicator", statusBarIcons.getBatteryIndicator());
        //TODO latent code for RSSI
//        outState.putInt("saveImageViewRSSIIndicator", topIcons.getRSSIIndicator());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
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
        //TODO latent code for RSSI
//        topIcons.setRSSIIndicator(savedInstanceState.getInt("saveImageViewRSSIIndicator"));
    }

    public class CGMStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_RESPONSE = "com.mSyncingServiceIntent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get response messages from broadcast
            int responseSGV = intent.getIntExtra(SyncingService.RESPONSE_SGV, -1);
            String trendSymbol = Constants.TREND_ARROW_VALUES.values()[intent.getIntExtra(SyncingService.RESPONSE_TREND,0)].Symbol();
            long responseSGVTimestamp = intent.getLongExtra(SyncingService.RESPONSE_TIMESTAMP,-1L);
            boolean responseUploadStatus = intent.getBooleanExtra(SyncingService.RESPONSE_UPLOAD_STATUS, false);
            long responseNextUploadTime = intent.getLongExtra(SyncingService.RESPONSE_NEXT_UPLOAD_TIME, -1);
            long responseDisplayTime = intent.getLongExtra(SyncingService.RESPONSE_DISPLAY_TIME, new Date().getTime());
            lastRecordTime = responseSGVTimestamp;
            int rssi = intent.getIntExtra(SyncingService.RESPONSE_RSSI, -1);
            int rcvrBat = intent.getIntExtra(SyncingService.RESPONSE_BAT, -1);
            String json = intent.getStringExtra(SyncingService.RESPONSE_JSON);

            String responseSGVStr=(responseSGV!=-1)?String.valueOf(responseSGV)+" "+trendSymbol:
                    (Constants.SPECIALBGVALUES.isSpecialValue(responseSGV))?Constants.SPECIALBGVALUES.getEGVSpecialValue(responseSGV).toString():"---";

            // Reload d3 chart with new data
            if (json != null) {
                mJSONData = json;
                mWebView.loadUrl("javascript:updateData(" + mJSONData + ")");
            }

            // Update icons
            statusBarIcons.setUpload(responseUploadStatus);

            // Update UI with latest record information
            mTextSGV.setText(responseSGVStr);
            mTextSGV.setTag(responseSGVStr);
            String timeAgoStr = "---";
            Log.d(TAG,"Date: " + new Date().getTime());
            Log.d(TAG,"Response SGV Timestamp: " + responseSGVTimestamp);
            if (responseSGVTimestamp > 0) {
                timeAgoStr = Utils.getTimeString(new Date().getTime() - responseSGVTimestamp);
            }

            mTextTimestamp.setText(timeAgoStr);
            mTextTimestamp.setTag(timeAgoStr);

            long nextUploadTime = TimeConstants.FIVE_MINUTES_MS;

            if (responseNextUploadTime > TimeConstants.FIVE_MINUTES_MS) {
                // TODO: how should we handle this situation?
                Log.d(TAG, "Receiver's time is less than current record time, possible time change.");
                mTracker.send(new HitBuilders.EventBuilder("Main", "Time change").build());
            } else if (responseNextUploadTime > 0) {
                Log.d(TAG, "Setting next upload time to: " + responseNextUploadTime);
                nextUploadTime = responseNextUploadTime;
            } else {
                Log.d(TAG, "OUT OF RANGE: Setting next upload time to: " + nextUploadTime + " ms.");
            }

            if (Math.abs(new Date().getTime()-responseDisplayTime) >= TimeConstants.TWENTY_MINUTES_MS) {
                Log.w(TAG, "Receiver time is off by 20 minutes or more.");
                mTracker.send(new HitBuilders.EventBuilder("Main", "Time difference > 20 minutes").build());
                statusBarIcons.setTimeIndicator(false);
            } else {
                statusBarIcons.setTimeIndicator(true);
            }

            Log.d(TAG,"RSSI is "+rssi);
//            topIcons.setRSSIIndicator(rssi);
            statusBarIcons.setBatteryIndicator(rcvrBat);

            mHandler.removeCallbacks(syncCGM);
            mHandler.postDelayed(syncCGM, nextUploadTime);
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn())
                mHandler.postDelayed(updateTimeAgo,nextUploadTime/5);
        }
    }

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                statusBarIcons.setUSB(false);
                statusBarIcons.setUpload(false);
                mHandler.removeCallbacks(syncCGM);
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                statusBarIcons.setUSB(true);
                Log.d(TAG, "Starting syncing on USB attached...");
                // TODO: 2nd parameter should be static constant from intent service
                SyncingService.startActionSingleSync(mContext, 5);
                //TODO: consider getting permission programmatically instead of user prompted
                //if decided to need to add android.permission.USB_PERMISSION in manifest
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                batLevel = intent.getIntExtra("level", 0);
            }
        }
    };

    BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            Log.d(TAG,"Intent => " + intent.getAction() + " received.");
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mHandler.post(updateTimeAgo);
                Log.d(TAG, "Updating time ago");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mHandler.removeCallbacks(updateTimeAgo);
                Log.d(TAG, "Disable updating of time ago");
            }
        }
    };

    // Runnable to start service as needed to sync with mCGMStatusReceiver and cloud
    public Runnable syncCGM = new Runnable() {
        public void run() {
            // TODO: 2nd parameter should be static constant from intent service
            SyncingService.startActionSingleSync(mContext, 5);
        }
    };

    public Runnable updateTimeAgo = new Runnable() {
        @Override
        public void run() {
            long delta = new Date().getTime() - lastRecordTime;
            if (lastRecordTime == 0) delta = 0;

            String timeAgoStr= "";

            if (delta <= 0 && lastRecordTime != -1) {
                timeAgoStr = "Time change detected";
            }
            else if (lastRecordTime==-1) {
                timeAgoStr = "---";
            }
            else {
                timeAgoStr = Utils.getTimeString(delta);
            }
            mTextTimestamp.setText(timeAgoStr);
            mHandler.removeCallbacks(updateTimeAgo);
            mHandler.postDelayed(updateTimeAgo, TimeConstants.ONE_MINUTE_MS);
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
            SyncingService.startActionSingleSync(getApplicationContext(), 20);
        } else if (id == R.id.close_settings) {
            mHandler.removeCallbacks(syncCGM);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public class StatusBarIcons {
        private ImageView mImageViewUSB;
        private ImageView mImageViewUpload;
        private ImageView mImageViewTimeIndicator;
        private ImageView mImageViewRSSI;
        private ImageView mImageRcvrBattery;
        private boolean usbActive;
        private boolean uploadActive;
        private boolean displayTimeSync;
        private int batteryLevel;
        private int rssi;

        StatusBarIcons(){
            mImageViewUSB = (ImageView) findViewById(R.id.imageViewUSB);
            mImageViewUpload = (ImageView) findViewById(R.id.imageViewUploadStatus);
            mImageViewTimeIndicator = (ImageView) findViewById(R.id.imageViewTimeIndicator);

            //TODO latent code for RSSI
//            mImageViewRSSI = (ImageView) findViewById(R.id.imageViewRSSI);
//            mImageViewRSSI.setImageResource(R.drawable.rssi);

            mImageRcvrBattery = (ImageView) findViewById(R.id.imageViewRcvrBattery);
            mImageRcvrBattery.setImageResource(R.drawable.battery);

            setDefaults();

        }

        public void setDefaults(){
            setUSB(false);
            setUpload(false);
            setTimeIndicator(false);
            setBatteryIndicator(0);
            //TODO latent code for RSSI
//            setRSSIIndicator(-1);
        }

        public void setUSB(boolean active){
            usbActive=active;
            if (active) {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_connected);
                mImageViewUSB.setTag(R.drawable.ic_usb_connected);
            } else {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_disconnected);
                mImageViewUSB.setTag(R.drawable.ic_usb_disconnected);
            }
        }

        public void setUpload(boolean active){
            uploadActive=active;
            if (active) {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_success);
                mImageViewUpload.setTag(R.drawable.ic_upload_success);
            } else {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_fail);
                mImageViewUpload.setTag(R.drawable.ic_upload_fail);
            }
        }

        public void setTimeIndicator(boolean active){
            displayTimeSync = active;
            if (active) {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_good);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_good);
            } else {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_bad);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_bad);
            }
        }

        public void setBatteryIndicator(int batLvl){
            batteryLevel = batLvl;
            mImageRcvrBattery.setImageLevel(batteryLevel);
            mImageRcvrBattery.setTag(batteryLevel);
        }

        public void setRSSIIndicator(int r){
            rssi = r;
            mImageViewRSSI.setImageLevel(rssi);
            mImageViewRSSI.setTag(rssi);
        }

        public boolean getUSB(){
            return usbActive;
        }

        public boolean getUpload(){
            return uploadActive;
        }

        public boolean getTimeIndicator(){
            return displayTimeSync;
        }

        public int getBatteryIndicator(){
            if (mImageRcvrBattery == null) {
                return 0;
            }
            return (Integer) mImageRcvrBattery.getTag();
        }

        public int getRSSIIndicator(){
            if (mImageViewRSSI==null)
                return 0;
            return (Integer) mImageViewRSSI.getTag();
        }
    }
}