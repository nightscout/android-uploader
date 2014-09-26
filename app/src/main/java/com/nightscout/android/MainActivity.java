package com.nightscout.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.nightscout.android.dexcom.SyncingService;
import com.nightscout.android.settings.SettingsActivity;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();

    // Member components
    private CGMStatusReceiver mCGMStatusReceiver;
    private Handler mHandler = new Handler();
    private Context mContext;

    // UI components
    private WebView mWebView;
    private TextView mTextSGV;
    private TextView mTextTimestamp;
    private Button mButton;
    private ImageView mImageViewUSB;
    private ImageView mImageViewUpload;

    // TODO: should try and avoid use static
    public static int batLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup UI components
        setContentView(R.layout.activity_main);
        mTextSGV = (TextView) findViewById(R.id.sgValue);
        mTextTimestamp = (TextView) findViewById(R.id.timeAgo);
        mButton = (Button)findViewById(R.id.stopSyncingButton);
        mImageViewUSB = (ImageView) findViewById(R.id.imageViewUSB);
        mImageViewUpload = (ImageView) findViewById(R.id.imageViewUploadStatus);
        mImageViewUpload.setImageResource(R.drawable.ic_upload_fail);
        mImageViewUpload.setTag(R.drawable.ic_upload_fail);
        mWebView = (WebView)findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setBackgroundColor(0);
        mWebView.loadUrl("file:///android_asset/index.html");

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

        // If app started due to android.hardware.usb.action.USB_DEVICE_ATTACHED intent, start syncing
        Intent startIntent = getIntent();
        String action = startIntent.getAction();
        boolean g4Connected=false;
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            g4Connected=true;
        } else {
            // Iterate through devices and see if the dexcom is connected
            // Allowing us to start to start syncing if the G4 is already connected
            // vendor-id="8867" product-id="71" class="2" subclass="0" protocol="0"
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while(deviceIterator.hasNext()){
                UsbDevice device = deviceIterator.next();
                if (device.getVendorId()==8867 && device.getProductId()==71
                        && device.getDeviceClass()==2 && device.getDeviceSubclass()==0
                        && device.getDeviceProtocol()== 0){
                    g4Connected=true;
                }
            }
        }

        if (g4Connected) {
            mImageViewUSB.setImageResource(R.drawable.ic_usb_connected);
            mImageViewUSB.setTag(R.drawable.ic_usb_connected);
            Log.d(TAG, "Starting syncing in OnCreate...");
            // TODO: 2nd parameter should be static constant from intent service
            SyncingService.startActionSingleSync(mContext, 1);
        } else {
            mImageViewUSB.setTag(R.drawable.ic_usb_disconnected);
        }

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(syncCGM);
                Log.d(TAG, "Starting 2 day syncing onClick...");
                // TODO: 2nd parameter should be static constant from intent service
                SyncingService.startActionSingleSync(mContext, 20);
            }
        });
        GoogleAnalytics.getInstance(getApplicationContext()).dispatchLocalHits();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        super.onDestroy();
        unregisterReceiver(mCGMStatusReceiver);
        unregisterReceiver(mDeviceStatusReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
        outState.putString("saveTextButton", mButton.getText().toString());
        outState.putInt("saveImageViewUSB", (Integer) mImageViewUSB.getTag());
        outState.putInt("saveImageViewUpload", (Integer) mImageViewUpload.getTag());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
        mButton.setText(savedInstanceState.getString("saveTextButton"));
        mImageViewUSB.setImageResource(savedInstanceState.getInt("saveImageViewUSB"));
        mImageViewUSB.setTag(savedInstanceState.getInt("saveImageViewUSB"));
        mImageViewUpload.setImageResource(savedInstanceState.getInt("saveImageViewUpload"));
        mImageViewUpload.setTag(savedInstanceState.getInt("saveImageViewUpload"));
    }

    public class CGMStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_RESPONSE = "com.mSyncingServiceIntent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get response messages from broadcast
            String responseString = intent.getStringExtra(SyncingService.RESPONSE_SGV);
            String responseMessage = intent.getStringExtra(SyncingService.RESPONSE_TIMESTAMP);
            boolean responseUploadStatus = intent.getBooleanExtra(SyncingService.RESPONSE_UPLOAD_STATUS, false);
            int responseNextUploadTime = intent.getIntExtra(SyncingService.RESPONSE_NEXT_UPLOAD_TIME, -1);

            if (responseUploadStatus) {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_success);
                mImageViewUpload.setTag(R.drawable.ic_upload_success);
            } else {
                mImageViewUpload.setImageResource(R.drawable.ic_upload_fail);
                mImageViewUpload.setTag(R.drawable.ic_upload_fail);
            }
            // Update UI with latest record information
            mTextSGV.setText(responseString);
            mTextTimestamp.setText(responseMessage);

            int nextUploadTime = TimeConstants.FIVE_MINUTES_MS;

            if (responseNextUploadTime > TimeConstants.FIVE_MINUTES_MS) {
                Log.d(TAG, "Receiver's time is less than current record time, possible time change.");
            } else if (responseNextUploadTime > 0) {
                Log.d(TAG, "Setting next upload time to: " + responseNextUploadTime);
                nextUploadTime = responseNextUploadTime;
            } else {
                Log.d(TAG, "OUT OF RANGE: Setting next upload time to: " + nextUploadTime + " ms.");
            }

            mHandler.removeCallbacks(syncCGM);
            mHandler.postDelayed(syncCGM, nextUploadTime);
        }
    }

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_disconnected);
                mImageViewUSB.setTag(R.drawable.ic_usb_disconnected);
                mImageViewUpload.setImageResource(R.drawable.ic_upload_fail);
                mImageViewUpload.setTag(R.drawable.ic_upload_fail);
                mHandler.removeCallbacks(syncCGM);
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                mImageViewUSB.setImageResource(R.drawable.ic_usb_connected);
                mImageViewUSB.setTag(R.drawable.ic_usb_connected);
                Log.d(TAG, "Starting syncing on USB attached...");
                // TODO: 2nd parameter should be static constant from intent service
                SyncingService.startActionSingleSync(mContext, 1);
                //TODO: consider getting permission programmatically instead of user prompted
                //if decided to need to add android.permission.USB_PERMISSION in manifest
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                batLevel = intent.getIntExtra("level", 0);
            }
        }
    };

    // Runnable to start service as needed to sync with mCGMStatusReceiver and cloud
    public Runnable syncCGM = new Runnable() {
        public void run() {
            // TODO: 2nd parameter should be static constant from intent service
            SyncingService.startActionSingleSync(mContext, 1);
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
        } else if (id == R.id.close_settings) {
            mHandler.removeCallbacks(syncCGM);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
