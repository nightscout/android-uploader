package com.nightscout.android.drivers;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
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

import com.nightscout.android.R;
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import net.tribe7.common.primitives.Bytes;

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

    public final static int CONNECT_TIMEOUT = 30000;
    public final static int READ_TIMEOUT = 5000;
    public final static int WRITE_TIMEOUT = 5000;

    private Action1<Boolean> connectionStateListener;

    // Bluetooth connection state variables
    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    private final String RECONNECT_INTENT = "org.nightscout.uploader.RECONNECT";

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

    private Boolean shouldBeOpen = false;

    public BluetoothTransport(Context context) {
        this.mContext = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//        mContext.registerReceiver(bluetoothStatusChangeReceiver, filter);
    }

    private AlarmManager alarmManager;
    private PendingIntent reconnectPendingIntent;

    private BroadcastReceiver reconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver bluetoothStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Log.d(TAG, "Bluetooth toggled...");
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {
                    try {
                        boolean tmp = shouldBeOpen;
                        close();
                        shouldBeOpen = tmp;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_ON) {
                    try {
                        if (shouldBeOpen) {
                            open();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public void open() throws IOException {
        shouldBeOpen = true;
        Log.d(TAG, "Starting open");
        AndroidPreferences prefs = new AndroidPreferences(mContext);
        mBluetoothDeviceAddress = prefs.getBtAddress();
        if (mBluetoothDeviceAddress.equals("")) {
            return;
        }
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mPairReceiver, bondIntent);
        final IntentFilter reconnectIntent = new IntentFilter(RECONNECT_INTENT);
        mContext.registerReceiver(reconnectReceiver, reconnectIntent);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(bluetoothStatusChangeReceiver, filter);


        attemptConnection();

        while (System.currentTimeMillis() + CONNECT_TIMEOUT < System.currentTimeMillis()) {
//            if (finalCallback) {
            if (finalCallback && authenticated && readNotifySet && mConnectionState == STATE_CONNECTED) {
                break;
            }
        }
        if (System.currentTimeMillis() + CONNECT_TIMEOUT < System.currentTimeMillis()) {
            Log.e(TAG, "Timeout while opening BLE connection to receiver");
            throw new IOException("Timeout while opening BLE connection to receiver");
        }
        Log.d(TAG, "Successfully made it to the end of open");
//        Observable.just(true).subscribe(connectionStateListener);
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "Closing connection");
        if (mBluetoothGatt == null) {
            return;
        }
        shouldBeOpen = false;
        mContext.unregisterReceiver(mPairReceiver);
        mContext.unregisterReceiver(reconnectReceiver);
        mContext.unregisterReceiver(bluetoothStatusChangeReceiver);
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
        readNotifySet = false;
        authenticated = false;
        finalCallback = false;
        Log.d(TAG, "Bluetooth has been disconnected.");
//        Observable.just(false).subscribe(connectionStateListener);
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {

        if (!isConnected()) {
            Log.e(TAG, "Unable to write to device. Device not connected");
            throw new IOException("Unable to write to device. Device not connected");
        }
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
        if (!isConnected()) {
            Log.d(TAG, "Unable to read. Disconnected from receiver.");
            throw new IOException("Disconnected from device. Unable to read");
        }

        long startTime = System.currentTimeMillis();
        while (startTime + timeoutMillis > System.currentTimeMillis()) {
            if (asyncReader.getResponse().length >= size) {
                break;
            }
        }
        if (startTime + timeoutMillis < System.currentTimeMillis()) {
            Log.d(TAG, "Timeout occurred while reading");
            throw new IOException("Timeout while reading from bluetooth address " + device.getAddress());
        }
        Log.d(TAG, "From asyncReader observable: " + Utils.bytesToHex(asyncReader.getResponse()));
        return asyncReader.getResponse();
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    public void registerConnectionListener(Action1<Boolean> connectionListener) {
        connectionStateListener = connectionListener;
    }


    public void attemptConnection() throws IOException {

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
                if (!mBluetoothDeviceAddress.equals("")) {
                    connect(mBluetoothDeviceAddress);
                } else {
                    Log.d(TAG, "Bluetooth device of interest can not be found.");
                }
            }

        } else {
            Log.d(TAG, "Bluetooth is disabled");
            EventReporter reporter = AndroidEventReporter.getReporter(mContext);
            reporter.report(EventType.DEVICE, EventSeverity.WARN, mContext.getString(R.string.warn_disabled_bluetooth));
        }

//        if (device == null) {
//            throw new IOException("Unable to open connection to receiver");
//        }
//        int state = mBluetoothManager.getConnectionState(device, BluetoothGatt.GATT);
//        if ((state != BluetoothProfile.STATE_CONNECTED) && (state != BluetoothProfile.STATE_CONNECTING)) {
//            throw new IOException("Unable to open connection to receiver");
//        }
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
                device = bluetoothDevice;
                mBluetoothGatt = device.connectGatt(mContext.getApplicationContext(), true, mGattCallback);
                if (isConnected()) {
                    Log.d(TAG, "Bluetooth device connected.");
                }
                return isConnected();
//                    return mBluetoothGatt.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED;

//                    return true;
            }
        }

        device = mBluetoothAdapter.getRemoteDevice(address);
        bondDevice();
        Log.w(TAG, "Trying to create a new connection with an unbonded device.");
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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                device = mBluetoothGatt.getDevice();
                mConnectionState = STATE_CONNECTED;
                Log.w(TAG, "Connected to GATT server.");
                Observable.just(true).subscribe(connectionStateListener);
                Log.w(TAG, "discovering services");
                if (!mBluetoothGatt.discoverServices()) {
                    Log.w(TAG, "discovering failed");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected from GATT server. Should be open: " + shouldBeOpen);
                Observable.just(false).subscribe(connectionStateListener);
                mConnectionState = STATE_DISCONNECTED;
//                if (shouldBeOpen) {
                    Log.w(TAG, "Connection was unexpectedly lost. Attempting to reconnect");
                    delayedReconnect(15000);
//                }

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
            } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
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
        Log.d(TAG, "Attempting to bond with remote device");
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mPairReceiver, bondIntent);

        if (!device.createBond()) {
            Log.d(TAG, "Problem creating bond");
        }
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

    public void reconnect() throws IOException {
        try {
            Log.d(TAG, "Attempting to close connection");
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Attempting to re-open connection");
        open();
//        attemptConnection();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void delayedReconnect(long millis) {
        Log.d(TAG, "Setting reconnecting in " + millis + " ms from now.");
        Intent reconnectIntent = new Intent(RECONNECT_INTENT);
        reconnectPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 1, reconnectIntent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, reconnectPendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, reconnectPendingIntent);
        }
    }

    public void cancelReconnect() {
        Log.d(TAG, "Canceling reconnect.");
        alarmManager.cancel(reconnectPendingIntent);
    }

}