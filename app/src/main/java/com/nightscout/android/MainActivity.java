package com.nightscout.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.dexcom.SyncingService;
import com.nightscout.android.dexcom.Utils;
import com.nightscout.android.settings.SettingsActivity;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

import java.util.Date;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();

    // Receivers
    private CGMStatusReceiver mCGMStatusReceiver;

    // Member components
    private Handler mHandler = new Handler();
    private Context mContext;
    private String mJSONData;
    private long lastRecordTime = 0;

    // Analytics tracker
    Tracker tracker;

    // UI components
    private WebView mWebView;
    private TextView mTextSGV;
    private TextView mTextTimestamp;
    private Button mTwoDaySyncButton;
    TopIcons topIcons;

    // TODO: should try and avoid use static
    public static int batLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracker = ((Nightscout) getApplicationContext()).getTracker();

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

        initUI(savedInstanceState);

        // If app started due to android.hardware.usb.action.USB_DEVICE_ATTACHED intent, start syncing
        Intent startIntent = getIntent();
        String action = startIntent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || SyncingService.isG4Connected(getApplicationContext())) {
            topIcons.setUSB(true);
            Log.d(TAG, "Starting syncing in OnCreate...");
            // TODO: 2nd parameter should be static constant from intent service
            SyncingService.startActionSingleSync(mContext, 5);
        } else {
            // reset the top icons to their default state
            topIcons.setDefaults();
        }
    }

    private void initUI(Bundle savedInstanceState){
        // Setup UI components
        setContentView(R.layout.activity_main);
        if (mTextSGV==null)
            mTextSGV = (TextView) findViewById(R.id.sgValue);
        if (mTextTimestamp==null)
            mTextTimestamp = (TextView) findViewById(R.id.timeAgo);
        if (mWebView==null) {
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
        }
        topIcons = new TopIcons();

        mTwoDaySyncButton = (Button)findViewById(R.id.twoDaySyncButton);

        mTwoDaySyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(syncCGM);
                Log.d(TAG, "Starting 2 day syncing onClick...");
                // TODO: 2nd parameter should be static constant from intent service
                SyncingService.startActionSingleSync(mContext, 20);
            }
        });

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
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
        outState.putString("saveTextButton", mTwoDaySyncButton.getText().toString());
        outState.putBoolean("saveImageViewUSB", topIcons.getUSB());
        outState.putBoolean("saveImageViewUpload", topIcons.getUpload());
        outState.putBoolean("saveImageViewTimeIndicator", topIcons.getTimeIndicator());
        outState.putInt("saveImageViewBatteryIndicator", topIcons.getBatteryIndicator());
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
        mTwoDaySyncButton.setText(savedInstanceState.getString("saveTextButton"));
        topIcons.setUSB(savedInstanceState.getBoolean("saveImageViewUSB"));
        topIcons.setUpload(savedInstanceState.getBoolean("saveImageViewUpload"));
        topIcons.setTimeIndicator(savedInstanceState.getBoolean("saveImageViewTimeIndicator"));
        topIcons.setBatteryIndicator(savedInstanceState.getInt("saveImageViewBatteryIndicator"));
        //TODO latent code for RSSI
//        topIcons.setRSSIIndicator(savedInstanceState.getInt("saveImageViewRSSIIndicator"));
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig){
//        super.onConfigurationChanged(newConfig);
//        Log.d(TAG,"onConfigurationChanged called");
//        initUI(null);
//    }

    public class CGMStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_RESPONSE = "com.mSyncingServiceIntent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get response messages from broadcast
            String responseSGV = intent.getStringExtra(SyncingService.RESPONSE_SGV);
            long responseSGVTimestamp = intent.getLongExtra(SyncingService.RESPONSE_TIMESTAMP,-1);
            boolean responseUploadStatus = intent.getBooleanExtra(SyncingService.RESPONSE_UPLOAD_STATUS, false);
            int responseNextUploadTime = intent.getIntExtra(SyncingService.RESPONSE_NEXT_UPLOAD_TIME, -1);
            long responseDisplayTime = intent.getLongExtra(SyncingService.RESPONSE_DISPLAY_TIME,new Date().getTime());
            lastRecordTime=responseSGVTimestamp;
            int rssi = intent.getIntExtra(SyncingService.RESPONSE_RSSI,-1);
            int rcvrBat = intent.getIntExtra(SyncingService.RESPONSE_BAT,-1);

            String json = intent.getStringExtra(SyncingService.RESPONSE_JSON);

            if (json != null) {
                mJSONData = json;
                mWebView.loadUrl("javascript:updateData(" + mJSONData + ")");
            }

            if (responseUploadStatus) {
                topIcons.setUpload(true);
            } else {
                topIcons.setUpload(false);
            }
            // Update UI with latest record information
            mTextSGV.setText(responseSGV);
            mTextSGV.setTag(responseSGV);
            String timeAgoStr = Utils.getTimeString(new Date().getTime()-responseSGVTimestamp);
            mTextTimestamp.setText(timeAgoStr);
            mTextTimestamp.setTag(timeAgoStr);

            int nextUploadTime = TimeConstants.FIVE_MINUTES_MS;

            if (responseNextUploadTime > TimeConstants.FIVE_MINUTES_MS) {
                // TODO how should we handle this situation?
                Log.d(TAG, "Receiver's time is less than current record time, possible time change.");
                tracker.send(new HitBuilders.EventBuilder("Main","Time change").build());
            } else if (responseNextUploadTime > 0) {
                Log.d(TAG, "Setting next upload time to: " + responseNextUploadTime);
                nextUploadTime = responseNextUploadTime;
            } else {
                Log.d(TAG, "OUT OF RANGE: Setting next upload time to: " + nextUploadTime + " ms.");
            }

            if (Math.abs(new Date().getTime()-responseDisplayTime) >= TimeConstants.TWENTY_MINUTES_MS) {
                Log.w(TAG,"Receiver timeoffset");
                tracker.send(new HitBuilders.EventBuilder("Main","Time difference > 20 minutes").build());
                topIcons.setTimeIndicator(false);
            } else {
                topIcons.setTimeIndicator(true);
            }

            Log.d(TAG,"RSSI is "+rssi);
//            topIcons.setRSSIIndicator(rssi);
            topIcons.setBatteryIndicator(rcvrBat);

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
                topIcons.setUSB(false);
                topIcons.setUpload(false);
                mHandler.removeCallbacks(syncCGM);
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                topIcons.setUSB(true);
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
            Log.d(TAG,"Intent=>"+intent.getAction()+" received");
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mHandler.post(updateTimeAgo);
                Log.d(TAG,"Updating time ago");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mHandler.removeCallbacks(updateTimeAgo);
                Log.d(TAG,"Disable updating of time ago");
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

    // TODO remove this runnable callback when the screen goes off and reinstate it when it comes
    // back on to save battery.
    public Runnable updateTimeAgo = new Runnable() {
        @Override
        public void run() {
            long delta= new Date().getTime() - lastRecordTime;
            Log.d("updateTimeAgo","Delta: "+delta);
            Log.d("updateTimeAgo","lastRecordTime: "+lastRecordTime);
            String timeAgoStr="";
            if (delta<0)
                timeAgoStr="Time change detected";
            else
                timeAgoStr=Utils.getTimeString(delta);
            mTextTimestamp.setText(timeAgoStr);
            mHandler.removeCallbacks(updateTimeAgo);
            mHandler.postDelayed(updateTimeAgo,60000);
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
        } else if (id == R.id.force_sync) {
            SyncingService.startActionSingleSync(getApplicationContext(),1);
        } else if (id == R.id.close_settings) {
            mHandler.removeCallbacks(syncCGM);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public class TopIcons {
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

        TopIcons(){
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
            setBatteryIndicator(-1);
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
            displayTimeSync=active;
            if (active) {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_good);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_good);
            } else {
                mImageViewTimeIndicator.setImageResource(R.drawable.ic_clock_bad);
                mImageViewTimeIndicator.setTag(R.drawable.ic_clock_bad);
            }
        }

        public void setBatteryIndicator(int batLvl){
            batteryLevel=batLvl;
            mImageRcvrBattery.setImageLevel(batteryLevel);
            mImageRcvrBattery.setTag(batteryLevel);
        }

        public void setRSSIIndicator(int r){
            rssi=r;
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
            return (Integer) mImageRcvrBattery.getTag();
        }

        public int getRSSIIndicator(){
            return (Integer) mImageViewRSSI.getTag();
        }

    }
}