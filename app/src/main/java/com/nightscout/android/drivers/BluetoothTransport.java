package com.nightscout.android.drivers;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.google.common.primitives.Bytes;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.DeviceTransport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import rx.Observable;
import rx.functions.Action1;

// NOTE: This code was ported from: https://github.com/StephenBlackWasAlreadyTaken/xDrip-Experimental version 3e890bab21c6d2938faf8711d9664afa2064ba61
public class BluetoothTransport implements DeviceTransport {

    private final String TAG = BluetoothTransport.class.getSimpleName();

    // Bluetooth classes
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice device;
    private String mBluetoothDeviceAddress;
    private AsyncReader asyncReader;
    private boolean readNotifySet = false;
    private boolean authenticated = false;
    private boolean finalCallback = false;

    // Bluetooth connection state variables
    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    // Current Bluetooth connection state
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    // Bluetooth service and characteristic variables
    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mAuthenticationCharacteristic;
    private BluetoothGattCharacteristic mSendDataCharacteristic;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;
    private BluetoothGattCharacteristic mResponseCharacteristic;
    private BluetoothGattCharacteristic mHeartBeatCharacteristic;

    // Header bytes for BLE messages
    private byte MESSAGE_INDEX = 0x01;
    private byte MESSAGE_COUNT = 0x01;

    // Member context variable
    private Context mContext;

    public BluetoothTransport(Context context) {
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.mContext = context;
        context.registerReceiver(mPairReceiver, bondIntent);
    }

    @Override
    public void open() throws IOException {
        AndroidPreferences prefs = new AndroidPreferences(mContext);
        mBluetoothDeviceAddress = prefs.getBtAddress();
        attemptConnection();

        while (System.currentTimeMillis() + 5000 > System.currentTimeMillis()) {
            if (finalCallback && authenticated && readNotifySet && mConnectionState == STATE_CONNECTED) {
                break;
            }
        }
        if (System.currentTimeMillis() + 5000 < System.currentTimeMillis()) {
            throw new IOException("Timeout while opening BLE connection to receiver");
        }
    }

    @Override
    public void close() throws IOException {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
        readNotifySet = false;
        authenticated = false;
        finalCallback = false;
        Log.d(TAG, "Bluetooth has been disconnected.");
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {

        // TODO: The bluetooth protocol has 2 additional bytes, a message index and message count,
        // right now we don't use any messages that have any index or count > 1.  But a function to
        // handle should be introduced.
        byte[] bytes = new byte[src.length + 2];
        bytes[0] = MESSAGE_INDEX;
        bytes[1] = MESSAGE_COUNT;
        System.arraycopy(src, 0, bytes, 2, src.length);

        Log.d(TAG, "Writing: " + Utils.bytesToHex(bytes));
        mSendDataCharacteristic.setValue(bytes);

        if (mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic)) {
            Log.d(TAG, "Write was successful.");
        }

        asyncReader = new AsyncReader();
        return src.length;
    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        return 0;
    }

    @Override
    public byte[] read(int size, int timeoutMillis) throws IOException {
        long startTime = System.currentTimeMillis();
        while (startTime + timeoutMillis > System.currentTimeMillis()) {
            if (asyncReader.getResponse().length >= size) {
                break;
            }
        }
        if (startTime + timeoutMillis > System.currentTimeMillis()) {
            Log.d(TAG, "Timeout occured while reading");
        }
        Log.d(TAG, "From asyncReader observable: " + Utils.bytesToHex(asyncReader.getResponse()));
        return asyncReader.getResponse();
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public boolean isConnected(int vendorId, int productId, int deviceClass, int subClass, int protocol) {
        return isConnected();
    }


    public void attemptConnection() {

        mConnectionState = STATE_DISCONNECTED;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothManager != null && mBluetoothAdapter.isEnabled()) {

            if (device != null) {

                // Check for a connected Share device
                for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                    if (bluetoothDevice.getAddress().compareTo(device.getAddress()) == 0) {
                        mConnectionState = STATE_CONNECTED;
                        Log.d(TAG, "Bluetooth device is connected.");
                    }
                }
            }

            if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {

                // Check if a previously connected device is range
                if (mBluetoothDeviceAddress != null && mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress) != null) {
                    connect(mBluetoothDeviceAddress);
                } else {
                    Log.d(TAG, "Bluetooth device of interest can not be found.");
                }
            }

        } else {
            Log.d(TAG, "Bluetooth is disabled");
        }

        Log.d(TAG, "Connection state: " + mConnectionState);
    }

    public boolean connect(final String address) {

        Log.d(TAG, "Attempting to connect with device with address: " + address);

        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothGatt != null) {
            Log.d(TAG, "Bluetooth Gatt is not null, so closing...");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Log.d(TAG, "Bluetooth Gatt closed.");
        }

        /////////////////////////////////////////////////////////////////////////////////////////////
        // needs some cleanup
        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
                Log.d(TAG, "Bluetooth device found and already bonded, so going to connect...");
                if (mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress()) != null) {
                    device = bluetoothDevice;
                    mBluetoothGatt = device.connectGatt(mContext.getApplicationContext(), false, mGattCallback);
                    Log.d(TAG, "Bluetooth device connected.");
                    return true;
                }
            }
        }

        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }
        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothGatt = device.connectGatt(mContext.getApplicationContext(), false, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        /////////////////////////////////////////////////////////////////////////////////////////////
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void authenticateConnection() {

        Log.w(TAG, "Trying to authenticate the share...");

        AndroidPreferences prefs = new AndroidPreferences(mContext);
        String receiverSn = prefs.getShareSerial().toUpperCase() + "000000";
        Log.d(TAG, "Receiver serial: " + receiverSn + "(" + Utils.bytesToHex(receiverSn.getBytes(StandardCharsets.US_ASCII)) + ")");

        if (receiverSn.compareTo("SM00000000000000") == 0 || receiverSn.equals("")) {
            // If they have not set their serial number, don't bond!
            return;
        }

        byte[] bondKey = (receiverSn).getBytes(StandardCharsets.US_ASCII);

        if (mBluetoothGatt != null) {
            if (mShareService != null) {
                mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode2);
            }
            if (mAuthenticationCharacteristic != null) {
                Log.d(TAG, "Authentication Characteristic found: " + mAuthenticationCharacteristic.toString());
                if (mAuthenticationCharacteristic.setValue(bondKey)) {
                    mBluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                }
            } else {
                Log.d(TAG, "Authentication Characteristic is NULL");
            }
        }
    }

    public void assignCharacteristics() {
        Log.d(TAG, "Setting #1 characteristics");
        mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver2);
        mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse2);
        mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command2);
        mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response2);
        mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat2);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        setCharacteristicNotification(characteristic, true);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic) {
        setCharacteristicIndication(characteristic, true);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting indication");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (!bondDevice.getAddress().equals(mBluetoothGatt.getDevice().getAddress())) {
                Log.d(TAG, "Bond state with wrong device, not going to pair.");
                return;
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Callback received: Bonded.");
                    authenticateConnection();
                } else if (state == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Callback received: Not Bonded.");
                } else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "Callback received:  Bonding in progress...");
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.w(TAG, "Gatt state change status: " + status + " new state: " + newState);
            writeStatusConnectionFailures(status);
            if (status == 133) {
                Log.e(TAG, "Got the status 133 bug, GROSS!!");
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                device = mBluetoothGatt.getDevice();
                mConnectionState = STATE_CONNECTED;
                Log.w(TAG, "Connected to GATT server.");

                Log.w(TAG, "discovering services");
                if (!mBluetoothGatt.discoverServices()) {
                    Log.w(TAG, "discovering failed");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.w(TAG, "Disconnected from GATT server.");
            } else {
                Log.w(TAG, "Gatt callback... strange state.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services discovered with status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService2);
                assignCharacteristics();
                authenticateConnection();
                setCharacteristicNotification(mReceiveDataCharacteristic);
                readNotifySet = true;
            } else {
                Log.w(TAG, "No Services Discovered.");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic Read " + characteristic.getUuid());
                if (mHeartBeatCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    Log.v(TAG, "Characteristic Read " + characteristic.getUuid() + " " + characteristic.getValue());
                    setCharacteristicNotification(mHeartBeatCharacteristic);
                }
                mBluetoothGatt.readCharacteristic(mHeartBeatCharacteristic);
            } else {
                Log.e(TAG, "Characteristic failed to read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                byte[] value = characteristic.getValue();
                if (value != null && asyncReader != null) {
                    Observable.just(value).subscribe(asyncReader);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                Log.d(TAG, "Characteristic onDescriptorWrite ch " + characteristic.getUuid());
                if (mHeartBeatCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    setCharacteristicIndication(mReceiveDataCharacteristic);
                }
                if (mReceiveDataCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    setCharacteristicIndication(mResponseCharacteristic);
                }
                if (mResponseCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    finalCallback = true;
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "characteristic wrote " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Wrote a characteristic successfully " + characteristic.getUuid());
                if (mAuthenticationCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    mBluetoothGatt.readCharacteristic(mHeartBeatCharacteristic);
                    authenticated = true;
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing Characteristic");
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void bondDevice() {
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mPairReceiver, bondIntent);

        device.createBond();
    }

    private void writeStatusConnectionFailures(int status) {
        if (status != 0) {
            Log.e(TAG, "ERROR: GATT_WRITE_NOT_PERMITTED " + (status & BluetoothGatt.GATT_WRITE_NOT_PERMITTED));
            Log.e(TAG, "ERROR: GATT_INSUFFICIENT_AUTHENTICATION " + (status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION));
            Log.e(TAG, "ERROR: GATT_REQUEST_NOT_SUPPORTED " + (status & BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED));
            Log.e(TAG, "ERROR: GATT_INSUFFICIENT_ENCRYPTION " + (status & BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION));
            Log.e(TAG, "ERROR: GATT_INVALID_OFFSET " + (status & BluetoothGatt.GATT_INVALID_OFFSET));
            Log.e(TAG, "ERROR: GATT_FAILURE " + (status & BluetoothGatt.GATT_FAILURE));
            Log.e(TAG, "ERROR: GATT_INVALID_ATTRIBUTE_LENGTH " + (status & BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH));
            Log.e(TAG, "ERROR: GATT_READ_NOT_PERMITTED " + (status & BluetoothGatt.GATT_READ_NOT_PERMITTED));
        }
    }

    private class AsyncReader implements Action1<byte[]> {
        private byte[] response = {};

        @Override
        public void call(byte[] bytes) {
            response = Bytes.concat(response, bytes);
        }

        public byte[] getResponse() {
            return response;
        }
    }
}