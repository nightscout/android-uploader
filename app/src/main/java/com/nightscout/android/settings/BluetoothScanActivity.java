package com.nightscout.android.settings;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nightscout.android.R;
import com.nightscout.android.preferences.AndroidPreferences;

import java.util.ArrayList;

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothScanActivity extends ListActivity {
    public static final long SCAN_PERIOD = 10000;
    private Handler handler;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean is_scanning;
    private BluetoothAdapter bluetooth_adapter;
    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);

//        ListView lv = (ListView)findViewById(android.R.id.list);

        final BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetooth_manager.getAdapter();
        mHandler = new Handler();

        if (bluetooth_adapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (bluetooth_manager == null) {
            Toast.makeText(this, "This device does not seem to support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetooth_manager.getAdapter().isEnabled()) {
                Toast.makeText(this, "Bluetooth is turned off on this device currently", Toast.LENGTH_LONG).show();
            } else {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
                }
            }
        }
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_scan, menu);
        if (!is_scanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(true);
                BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                Toast.makeText(this, "Scanning", Toast.LENGTH_LONG).show();
                if (bluetooth_manager == null) {
                    Toast.makeText(this, "This device does not seem to support bluetooth", Toast.LENGTH_LONG).show();
                } else {
                    if (!bluetooth_manager.getAdapter().isEnabled()) {
                        Toast.makeText(this, "Bluetooth is turned off on this device currently", Toast.LENGTH_LONG).show();
                    } else {
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    is_scanning = false;
                    bluetooth_adapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            is_scanning = true;
            bluetooth_adapter.startLeScan(mLeScanCallback);
        } else {
            is_scanning = false;
            bluetooth_adapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        Toast.makeText(this, R.string.connecting_to_device, Toast.LENGTH_LONG).show();
        AndroidPreferences prefs = new AndroidPreferences(getApplicationContext());
        prefs.setBluetoothDevice(device.getName(), device.getAddress());

        if (is_scanning) {
            bluetooth_adapter.stopLeScan(mLeScanCallback);
            is_scanning = false;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BluetoothScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.ledevice_listitem, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


}
