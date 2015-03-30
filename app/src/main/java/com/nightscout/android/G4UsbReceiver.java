package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.nightscout.core.drivers.DexcomG4;

import java.util.HashMap;

public class G4UsbReceiver extends BroadcastReceiver {
    private final String TAG = G4UsbReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                if (!isConnected(context)) {
                    Log.d(TAG, "Stopping syncing on USB attached...");
                    Intent syncIntent = new Intent(context, CollectorService.class);
//                    syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.NON_SYNC);
                    context.stopService(syncIntent);
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                if (isConnected(context)) {
                    Log.d(TAG, "Starting syncing on USB attached...");
                    Intent syncIntent = new Intent(context, CollectorService.class);
                    syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.STD_SYNC);
                    syncIntent.putExtra(CollectorService.NUM_PAGES, 1);
                    context.startService(syncIntent);
                }
                break;
        }
    }

    private boolean isConnected(Context context) {
        HashMap<String, UsbDevice> deviceList = ((UsbManager) context.getSystemService(Context.USB_SERVICE)).getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == DexcomG4.VENDOR_ID && device.getProductId() == DexcomG4.PRODUCT_ID &&
                    device.getDeviceClass() == DexcomG4.DEVICE_CLASS &&
                    device.getDeviceSubclass() == DexcomG4.DEVICE_SUBCLASS &&
                    device.getDeviceProtocol() == DexcomG4.PROTOCOL) {
                return true;
            }
        }
        return false;
    }
}
