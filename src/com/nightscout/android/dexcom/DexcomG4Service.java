package com.nightscout.android.dexcom;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nightscout.android.dexcom.USB.SerialInputOutputManager;
import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;
import com.nightscout.android.upload.UploadHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DexcomG4Service extends Service {

    private final String TAG = DexcomG4Activity.class.getSimpleName();

    private boolean initialRead = true;

    /**
     * The device currently in use, or {@code null}.
     */
    private UsbSerialDriver mSerialDevice;

    /**
     * The system's USB service.
     */
    public UsbManager mUsbManager;
    private UploadHelper uploader;
    private Handler mHandler = new Handler();

    private SerialInputOutputManager mSerialIoManager;
    private WifiManager wifiManager;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // connectToG4();
        mHandler.removeCallbacks(readAndUpload);
        mHandler.post(readAndUpload);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(readAndUpload);
        USBOn();
        doReadAndUpload();
        USBOn();

        stopIoManager();

        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }

    }

    //get the data upload it
    //if you don't have root, the On, Off commands won't do a thing - shouldn't break anything either
    private Runnable readAndUpload = new Runnable() {
        public void run() {

            try {
                uploader = new UploadHelper(getBaseContext());
                if (isConnected() && isOnline()) {

                    USBOn();
                    doReadAndUpload();
                    USBOff();

                    //displayMessage("Upload Complete");

                } else {
                    USBOn();
                    USBOff();
                    displayMessage("Upload Fail");
                }

            } catch (Exception e) {
                // ignore... for now - simply prevent service and activity from
                // losing its shit.
                USBOn();
                USBOff();
                e.printStackTrace();
            }
            mHandler.postDelayed(readAndUpload, 45000);
        }
    };

    protected void doReadAndUpload() {

        try {

            mSerialDevice = null;
            mSerialDevice = UsbSerialProber.acquire(mUsbManager);

            if (mSerialDevice != null) {
                startIoManager();
                mSerialDevice.open();

                //Go get the data
                DexcomReader dexcomReader = new DexcomReader(mSerialDevice);

                if (initialRead == true) {
                    // for first time on, read at least 2 days of data.  Each Dexcom read of EGV records
                    // is limited to 4 pages which is equivalent to 12 hours of contiguous data, so
                    // read 20 pages which is ~ 2.5 days.
                    List<EGVRecord> data = new ArrayList<EGVRecord>();
                    for(int i = 1; i <= 5; i++) {
                        dexcomReader.readFromReceiver(getBaseContext(), i);
                        for(int j = 0; j < dexcomReader.mRD.length; j++) { data.add(dexcomReader.mRD[j]); }
                    }
                    EGVRecord[] dataRecords = new EGVRecord[data.size()];
                    dataRecords = data.toArray(dataRecords);
                    uploader.execute(dataRecords);
                    initialRead = false;
                } else {
                    // just read most recent pages (consider only reading 1 page since only need latest value).
                    dexcomReader.readFromReceiver(getBaseContext(), 1);
                    uploader.execute(dexcomReader.mRD[dexcomReader.mRD.length - 1]);
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    //Interesting case: location with lousy wifi
                    //toggle it off to use cellular
                    //toggle back on for next try
                    public void run() {
                        Status dataUp = uploader.getStatus();
                        if (dataUp == Status.RUNNING) {
                            uploader.cancel(true);

                            if (wifiManager.isWifiEnabled()) {
                                wifiManager.setWifiEnabled(false);
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                wifiManager.setWifiEnabled(true);
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                }, 22500);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void USBOff() {
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            USBPower.PowerOff();
            Log.i(TAG, "USB OFF");
        } else {
            // displayMessage("Receiver Not Found");
            // android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    private void USBOn() {
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            USBPower.PowerOn();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "USB ON");
        } else {
            // displayMessage("Receiver Not Found");
            // android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    private boolean isConnected() {

        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        if (mSerialDevice == null) {
            //displayMessage("CGM Not Found...");
            this.stopSelf();
            return false; // yeah, I know
        }
        return true;

    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    private void displayMessage(String message) {
        Toast toast = Toast.makeText(getBaseContext(), message,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout toastLayout = (LinearLayout) toast.getView();
        TextView toastTV = (TextView) toastLayout.getChildAt(0);
        toastTV.setTextSize(20);
        toastTV.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        toast.show();

    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice);
            // mExecutor.submit(mSerialIoManager);
        }
    }

}
