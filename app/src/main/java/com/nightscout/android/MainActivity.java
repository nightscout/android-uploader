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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nightscout.android.dexcom.SyncingService;
import com.nightscout.android.settings.SettingsActivity;


public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();
    private CGMStatusReceiver receiver;
    private Handler mHandler = new Handler();
    private Context context;
    private Intent intent;

    // UI components
    private TextView mTextSGV;
    private TextView mTextTimestamp;
    private Button mButton;
    private ImageView mImageViewUSB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mTextSGV = (TextView) findViewById(R.id.sgValue);
        mTextTimestamp = (TextView) findViewById(R.id.timeAgo);
        mButton = (Button)findViewById(R.id.stopSyncingButton);
        mImageViewUSB = (ImageView) findViewById(R.id.imageViewUSB);

        context = getApplicationContext();
        intent = new Intent(this, SyncingService.class);

        // Register USB attached/detached intents
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);

        // Register Broadcast Receiver for response messages from intent service
        receiver = new CGMStatusReceiver();
        IntentFilter filter = new IntentFilter(CGMStatusReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver, filter);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: use constants for text value changes
                CharSequence test = mButton.getText();
                if (mButton.getText().equals("Start Syncing")) {
                    Log.d(TAG, "Starting syncing...");
                    SyncingService.startActionSync(context, "test", "test");
                    startService(intent);
                    mButton.setText("Stop Syncing");
                } else if (mButton.getText().equals("Stop Syncing")) {
                    Log.d(TAG, "Stopping syncing and removing callbacks.");
                    mHandler.removeCallbacks(syncCGM);
                    mButton.setText("Start Syncing");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        super.onDestroy();
        unregisterReceiver(receiver);
        mHandler.removeCallbacks(syncCGM);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
        outState.putString("saveTextButton", mButton.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
        mButton.setText(savedInstanceState.getString("saveTextButton"));
    }

    public class CGMStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_RESPONSE = "com.intent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            String responseString = intent.getStringExtra(SyncingService.RESPONSE_SGV);
            String responseMessage = intent.getStringExtra(SyncingService.RESPONSE_TIMESTAMP);
            int responseNextUploadTime = intent.getIntExtra(SyncingService.RESPONSE_NEXT_UPLOAD_TIME, 180000);

            // Update with latest record
            mTextSGV.setText(responseString);
            mTextTimestamp.setText(responseMessage);

            // Add status icon for usb connected
            if (responseString.equals("---")) {
                mImageViewUSB.setImageResource(R.drawable.usb_disconnected);
            } else {
                mImageViewUSB.setImageResource(R.drawable.usb_connected);
            }

            Log.d(TAG, "Setting next upload time to: " + responseNextUploadTime);
            mHandler.removeCallbacks(syncCGM);
            mHandler.postDelayed(syncCGM, responseNextUploadTime);
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "USB detached broadcast", Toast.LENGTH_SHORT).show();
//                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                if (device != null) {
//                    // call your method that cleans up and closes communication with the device
//                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "USB attached broadcast", Toast.LENGTH_SHORT).show();
                //TODO: consider getting permission programmatically instead of user prompted
                //if decided to need to add android.permission.USB_PERMISSION in manifest
            }
        }
    };

    public Runnable syncCGM = new Runnable() {
        public void run() {
            final Context context = getApplicationContext();
            SyncingService.startActionSync(context, "test", "test");
            startService(intent);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
