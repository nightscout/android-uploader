package com.nightscout.android.drivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.nightscout.core.drivers.AbstractUploaderDevice;

public class AndroidUploaderDevice extends AbstractUploaderDevice {
    private int uploaderBattery;
    private Context context;

    private AndroidUploaderDevice(Context context) {
        IntentFilter deviceStatusFilter = new IntentFilter();
        deviceStatusFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        this.context = context;
        this.context.registerReceiver(mDeviceStatusReceiver, deviceStatusFilter);
    }

    public int getBatteryLevel() {
        return uploaderBattery;
    }

    // TODO: This registers everytime. Need to fix
    public static AndroidUploaderDevice getUploaderDevice(Context context) {
        return new AndroidUploaderDevice(context);
    }

    public void close() {
        context.unregisterReceiver(mDeviceStatusReceiver);
    }

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                uploaderBattery = intent.getIntExtra("level", 0);
            }
        }
    };
}
