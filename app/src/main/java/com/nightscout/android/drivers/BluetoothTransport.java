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
import com.nightscout.core.drivers.G4ConnectionState;
import com.nightscout.core.events.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import net.tribe7.common.primitives.Bytes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    private Boolean finalCallback = false;
    private Queue<Request> mInitQueue;
    private boolean mInitInProgress;

    public final static int CONNECT_TIMEOUT = 60000;
    public final static int READ_TIMEOUT = 5000;
    public final static int WRITE_TIMEOUT = 5000;

    private Action1<G4ConnectionState> connectionStateListener;

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

    private G4ConnectionState connectionState = G4ConnectionState.CLOSED;

    // Header bytes for BLE messages
    private byte MESSAGE_INDEX = 0x01;
    private byte MESSAGE_COUNT = 0x01;

    // Member context variable
    private Context mContext;

    private Boolean shouldBeOpen = false;

    public BluetoothTransport(Context context) {
        this.mContext = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(bluetoothStatusChangeReceiver, filter);
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
                    boolean tmp = shouldBeOpen;
//                        close();
                    shouldBeOpen = tmp;
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
        setConnectionState(G4ConnectionState.CONNECTING);
        shouldBeOpen = true;
        Log.d(TAG, "Starting open");
        AndroidPreferences prefs = new AndroidPreferences(mContext);
        mBluetoothDeviceAddress = prefs.getBtAddress();
        if (mBluetoothDeviceAddress.equals("")) {
            setConnectionState(G4ConnectionState.CLOSED);
            throw new IOException("Invalid bluetooth address");
        }
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBondingBroadcastReceiver, bondIntent);
        final IntentFilter reconnectIntent = new IntentFilter(RECONNECT_INTENT);
        mContext.registerReceiver(reconnectReceiver, reconnectIntent);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(bluetoothStatusChangeReceiver, filter);

        attemptConnection();

        long endTime = System.currentTimeMillis() + CONNECT_TIMEOUT;
        while (System.currentTimeMillis() < endTime) {
            if (finalCallback) {
                Log.e(TAG, "Indicator of success found!");
//            if (finalCallback && authenticated && readNotifySet && mConnectionState == STATE_CONNECTED) {
                break;
            }
        }
        if (!finalCallback) {
            Log.e(TAG, "Timeout while opening BLE connection to receiver");
            setConnectionState(G4ConnectionState.CLOSED);
            throw new IOException("Timeout while opening BLE connection to receiver");
        }
        Log.d(TAG, "Successfully made it to the end of open");
        setConnectionState(G4ConnectionState.CONNECTED);
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "Closing connection");
        if (mBluetoothGatt == null) {
            return;
        }
        shouldBeOpen = false;
        mContext.unregisterReceiver(mBondingBroadcastReceiver);
        mContext.unregisterReceiver(reconnectReceiver);
        mContext.unregisterReceiver(bluetoothStatusChangeReceiver);
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
        if (!isConnected()) {
            Log.e(TAG, "Unable to write to device. Device not connected");
            setConnectionState(G4ConnectionState.CLOSED);
            throw new IOException("Unable to write to device. Device not connected");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
        setConnectionState(G4ConnectionState.WRITING);
        // TODO: The bluetooth protocol has 2 additional bytes, a message index and message count,
        // right now we don't use any messages that have any index or count > 1.  But a function to
        // handle should be introduced.
        byte[] bytes = new byte[src.length + 2];
        bytes[0] = MESSAGE_INDEX;
        bytes[1] = MESSAGE_COUNT;
        System.arraycopy(src, 0, bytes, 2, src.length);

        Log.d(TAG, "Writing: " + Utils.bytesToHex(bytes));
        mSendDataCharacteristic.setValue(bytes);

        asyncReader = new AsyncReader();
        if (mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic)) {
            Log.d(TAG, "Write was successfully initiated");
        }
        setConnectionState(G4ConnectionState.CONNECTED);
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
            setConnectionState(G4ConnectionState.CLOSED);
            throw new IOException("Disconnected from device. Unable to read");
        }
        setConnectionState(G4ConnectionState.READING);
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (endTime > System.currentTimeMillis()) {
            if (asyncReader.getResponse().length >= size) {
                break;
            }
        }
        if (endTime < System.currentTimeMillis()) {
            Log.d(TAG, "Timeout occurred while reading");
            throw new IOException("Timeout while reading from bluetooth address " + device.getAddress());
        }
        Log.d(TAG, "From asyncReader observable: " + Utils.bytesToHex(asyncReader.getResponse()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        }
        setConnectionState(G4ConnectionState.CONNECTED);
        return asyncReader.getResponse();
    }

    public boolean isConnected() {
        return mConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    public void registerConnectionListener(Action1<G4ConnectionState> connectionListener) {
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
                        setConnectionState(G4ConnectionState.CONNECTED);
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
            setConnectionState(G4ConnectionState.CLOSED);
            EventReporter reporter = AndroidEventReporter.getReporter(mContext);
            reporter.report(EventType.DEVICE, EventSeverity.WARN, mContext.getString(R.string.warn_disabled_bluetooth));
        }

        Log.d(TAG, "Connection state: " + mConnectionState);
    }

    public boolean connect(final String address) {

        Log.d(TAG, "Attempting to connect with device with address: " + address);

        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
            setConnectionState(G4ConnectionState.CLOSED);
            return false;
        }

        if (mBluetoothGatt != null) {
            Log.d(TAG, "Bluetooth Gatt is not null, so closing...");
            setConnectionState(G4ConnectionState.CLOSED);
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Log.d(TAG, "Bluetooth Gatt closed.");
        }

        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
                Log.d(TAG, "Bluetooth device found and already bonded, so going to connect...");
                device = bluetoothDevice;
                mBluetoothGatt = device.connectGatt(mContext.getApplicationContext(), true, mGattCallback);
                if (isConnected()) {
                    setConnectionState(G4ConnectionState.CONNECTED);
                    Log.d(TAG, "Bluetooth device connected.");
                }
                return isConnected();
            }
        }

        device = mBluetoothAdapter.getRemoteDevice(address);
//        bondDevice();
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private byte[] getHandshakeKey() {
        AndroidPreferences prefs = new AndroidPreferences(mContext);
        String receiverSn = prefs.getShareSerial().toUpperCase() + "000000";
        Log.d(TAG, "Receiver serial: " + receiverSn + "(" + Utils.bytesToHex(receiverSn.getBytes(StandardCharsets.US_ASCII)) + ")");

        if (receiverSn.compareTo("SM00000000000000") == 0 || receiverSn.equals("")) {
            // If they have not set their serial number, don't bond!
            return new byte[0];
        }

        return (receiverSn).getBytes(StandardCharsets.US_ASCII);
    }

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            // Skip other devices
            if (mBluetoothGatt == null || !device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
                return;

            Log.i(TAG, "Bond state changed for: " + device.getName() + " new state: " + bondState + " previous: " + previousBondState);

            switch (bondState) {
                case BluetoothDevice.BOND_BONDING:
                    Log.v(TAG, "Bond state: Bonding...");
//                    mCallbacks.onBondingRequired();
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.i(TAG, "Bond state: Bonded");
//                    mCallbacks.onBonded();

                    // Start initializing again.
                    // In fact, bonding forces additional, internal service discovery (at least on Nexus devices), so this method may safely be used to start this process again.
                    Log.v(TAG, "Discovering Services...");
                    Log.d(TAG, "gatt.discoverServices()");
                    mInitQueue = null;
                    mInitInProgress = true;
                    mInitQueue = initGatt(getHandshakeKey());
                    mBluetoothGatt.discoverServices();
                    break;
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String state = (newState == BluetoothProfile.STATE_CONNECTED) ? "connected" : "disconnected";
            String connectionStatus = (status == BluetoothGatt.GATT_SUCCESS) ? "success" : "fail";
            Log.w(TAG, "Gatt state change status: " + connectionStatus + " new state: " + state);
            Log.w(TAG, "Errors: " + writeStatusConnectionFailures(status));
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    setConnectionState(G4ConnectionState.CONNECTING);
                    mBluetoothGatt = gatt;
                    device = mBluetoothGatt.getDevice();
                    mConnectionState = STATE_CONNECTED;
                    Log.w(TAG, "Connected to GATT server.");
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mBluetoothGatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
                        Log.v(TAG, "Discovering Services...");
                        mBluetoothGatt.discoverServices();
                    }
                } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                    int bondState = gatt.getDevice().getBondState();
                    Log.w(TAG, "Insufficient authentication checking bond state (" + bondState + ")");
                    if (bondState == BluetoothDevice.BOND_NONE) {
                        Log.w(TAG, "Unbonded. Attempting to bond");
                        device = gatt.getDevice();
//                        bondDevice();
                    } else if (bondState == BluetoothDevice.BOND_BONDING) {
                        Log.w(TAG, "Seems to be stuck bonding. Attempting to initiate bond again");
//                        cancelBond();
//                        removeBond();
//                        bondDevice();
                    } else if (bondState == BluetoothDevice.BOND_BONDED) {
                        Log.e(TAG, "Reporting bonded but received insufficient authentication. Attempting to rebond");
//                        cancelBond();
//                        removeBond();
//                        bondDevice();
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected from GATT server. Should be open: " + shouldBeOpen);
                finalCallback = false;
                setConnectionState(G4ConnectionState.CLOSED);
                mConnectionState = STATE_DISCONNECTED;
                Log.w(TAG, "Connection was unexpectedly lost. Attempting to reconnect");

            } else {
                Log.w(TAG, "Gatt callback... strange state.");
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.e(TAG, "MTU = " + mtu + " status: " + status);
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services discovered with status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService2);
                Log.e(TAG, "Number of services: " + mShareService.getIncludedServices().size());
                Log.e(TAG, "Number of characteristics: " + mShareService.getCharacteristics().size());
                if (mShareService != null) {
                    Log.i(TAG, "Getting characteristics");

                    mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver2);
                    Log.e(TAG, "Properties that exist for send data: " + propertiesAvailable(mSendDataCharacteristic.getProperties()));
                    mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse2);
                    Log.e(TAG, "Properties that exist for receive data: " + propertiesAvailable(mReceiveDataCharacteristic.getProperties()));
                    mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command2);
                    Log.e(TAG, "Properties that exist for command: " + propertiesAvailable(mCommandCharacteristic.getProperties()));
                    mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response2);
                    Log.e(TAG, "Properties that exist for response: " + propertiesAvailable(mResponseCharacteristic.getProperties()));
                    mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat2);
                    Log.e(TAG, "Properties that exist for heartbeat: " + propertiesAvailable(mHeartBeatCharacteristic.getProperties()));
                    mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode2);
                    Log.e(TAG, "Properties that exist for auth: " + propertiesAvailable(mAuthenticationCharacteristic.getProperties()));
                    if (mAuthenticationCharacteristic == null) {
                        Log.e(TAG, "Something is wrong. Auth characteristic is null");
                    }
                    Log.d(TAG, "Authentication characteristic: " + mAuthenticationCharacteristic.getUuid().toString());

                    mInitInProgress = true;
                    mInitQueue = initGatt(getHandshakeKey());
                    nextRequest();
                    if (ensureServiceChangedEnabled(gatt)) {
                        Log.e(TAG, "Returning in onServices discovered");
                        return;
                    }
                } else {
                    Log.e(TAG, "Unsupported device");
                }
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
                    Log.v(TAG, "Characteristic Read " + characteristic.getUuid() + " value: " + Utils.bytesToHex(characteristic.getValue()));
//                    setCharacteristicNotification(mHeartBeatCharacteristic);
                    enableNotifications(mHeartBeatCharacteristic);
                } else {
                    mBluetoothGatt.readCharacteristic(mHeartBeatCharacteristic);
                }
//                nextRequest();
            } else {
                Log.e(TAG, "Characteristic failed to read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.v(TAG, "Characteristic changed (" + characteristic.getUuid().toString() + ")");
            UUID charUuid = characteristic.getUuid();
            if (charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                byte[] value = characteristic.getValue();
                if (value != null && asyncReader != null) {
                    Observable.just(value).subscribe(asyncReader);
                }
            }
            if (charUuid.compareTo(mHeartBeatCharacteristic.getUuid()) == 0) {
                Log.v(TAG, "Heartbeat: " + Utils.bytesToHex(characteristic.getValue()));
                mBluetoothGatt.readRemoteRssi();
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "RSSI: " + rssi + " Status: " + status);
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            Log.w(TAG, "Characteristic onDescriptorWrite ch " + characteristic.getUuid() + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                nextRequest();
            } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                int bondState = device.getBondState();
                if (bondState == BluetoothDevice.BOND_NONE) {
                    Log.w(TAG, "Unbonded. Attempting to bond");
                    device = gatt.getDevice();
//                    bondDevice();
                } else if (bondState == BluetoothDevice.BOND_BONDING) {
                    Log.w(TAG, "Seems to be stuck bonding. Attempting to initiate bond again");
//                    cancelBond();
//                    removeBond();
//                    bondDevice();
                } else if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "Reporting bonded but received insufficient authentication. Attempting to rebond");
//                    cancelBond();
//                    removeBond();
//                    bondDevice();
                }
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
                Log.w(TAG, "Errors: " + writeStatusConnectionFailures(status));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "characteristic wrote " + status);
            Log.w(TAG, "Errors: " + writeStatusConnectionFailures(status));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Wrote a characteristic successfully " + characteristic.getUuid());
                if (mAuthenticationCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    mBluetoothGatt.readCharacteristic(mHeartBeatCharacteristic);
                    authenticated = true;
                }
            } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                int bondState = device.getBondState();
                if (bondState == BluetoothDevice.BOND_NONE) {
                    Log.w(TAG, "Unbonded. Attempting to bond");
                    device = gatt.getDevice();
//                    bondDevice();
                } else if (bondState == BluetoothDevice.BOND_BONDING) {
                    Log.w(TAG, "Seems to be stuck bonding. Attempting to initiate bond again");
//                    cancelBond();
//                    removeBond();
//                    bondDevice();
                } else if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "Reporting bonded but received insufficient authentication. Attempting to rebond");
//                    cancelBond();
//                    removeBond();
//                    bondDevice();
                }
            } else {
                Log.e(TAG, "Unknown error writing Characteristic");
            }
            nextRequest();
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void bondDevice() {
        Log.d(TAG, "Attempting to bond with remote device");
        final IntentFilter bondIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBondingBroadcastReceiver, bondIntent);

        if (!device.createBond()) {
            Log.d(TAG, "Problem creating bond");
        }
    }

    public void removeBond() {
        Log.w(TAG, "Attempting to remove bond");
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void cancelBond() {
        Log.w(TAG, "Attempting to cancel bond");
        try {
            Method m = device.getClass()
                    .getMethod("cancelBondProcess", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }


    private List<String> writeStatusConnectionFailures(int status) {
        List<String> results = new ArrayList<>();
        if (status != 0) {
            Log.e(TAG, "Connection Failure status: " + status);
            if ((status & BluetoothGatt.GATT_WRITE_NOT_PERMITTED) != 0) {
                results.add("GATT_WRITE_NOT_PERMITTED");
            }
            if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) != 0) {
                results.add("GATT_INSUFFICIENT_AUTHENTICATION");
            }
            if ((status & BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) != 0) {
                results.add("GATT_REQUEST_NOT_SUPPORTED");
            }
            if ((status & BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) != 0) {
                results.add("GATT_INSUFFICIENT_ENCRYPTION");
            }
            if ((status & BluetoothGatt.GATT_INVALID_OFFSET) != 0) {
                results.add("GATT_INVALID_OFFSET");
            }
            if ((status & BluetoothGatt.GATT_FAILURE) != 0) {
                results.add("GATT_FAILURE");
            }
            if ((status & BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH) != 0) {
                results.add("GATT_INVALID_ATTRIBUTE_LENGTH");
            }
            if ((status & BluetoothGatt.GATT_READ_NOT_PERMITTED) != 0) {
                results.add("GATT_READ_NOT_PERMITTED");
            }
        }
        return results;
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

    private boolean ensureServiceChangedEnabled(final BluetoothGatt gatt) {
        if (gatt == null) {
            Log.d(TAG, "Returning false for ensureServiceChangedEnabled due to null gatt");
            return false;
        }

        // The Service Changed indications have sense only on bonded devices
        final BluetoothDevice device = gatt.getDevice();
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "Returning false for ensureServiceChangedEnabled due unbonded device");
            return false;
        }

        final BluetoothGattService gaService = gatt.getService(DexShareAttributes.GENERIC_ATTRIBUTE_SERVICE);
        if (gaService == null) {
            Log.d(TAG, "Returning false for ensureServiceChangedEnabled due being unable to read Generic attribute service");
            return false;
        }

        final BluetoothGattCharacteristic scCharacteristic = gaService.getCharacteristic(DexShareAttributes.SERVICE_CHANGED_CHARACTERISTIC);
        if (scCharacteristic == null) {
            Log.d(TAG, "Returning false for ensureServiceChangedEnabled due being unable to get service changed characteristic");
            return false;
        }

        return enableIndications(scCharacteristic);
    }

    protected final boolean enableIndications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null) {
            Log.d(TAG, "Returning false due to null gatt or characteristic");
            return false;
        }

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            Log.d(TAG, "Returning false because Indicate property does not seem to exist for " + characteristic.getUuid().toString() + ". Properties that exist: " + propertiesAvailable(properties));
            return false;
        }

        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            Log.v(TAG, "Enabling indications for " + characteristic.getUuid());
            Log.d(TAG, "gatt.writeDescriptor(" + DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG.toString() + ", value=0x02-00)");
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    protected final boolean enableNotifications(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null) {
            Log.e(TAG, "Returning false due to null gatt or characteristic");
            return false;
        }

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            Log.d(TAG, "Returning false because Notification property does not seem to exist for " + characteristic.getUuid().toString() + ". Properties that exist: " + propertiesAvailable(properties));

            return false;
        }

        gatt.setCharacteristicNotification(characteristic, true);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.v(TAG, "Enabling notifications for " + characteristic.getUuid());
            Log.d(TAG, "gatt.writeDescriptor(" + DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG + ", value=0x01-00)");
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    private boolean isPropertySet(int propertyToCheckFor, int properties) {
        return (properties & propertyToCheckFor) == propertyToCheckFor;
    }

    private List<String> propertiesAvailable(int properties) {
        List<String> availProperties = new ArrayList<>();
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_BROADCAST, properties)) {
            availProperties.add("Broadcast");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, properties)) {
            availProperties.add("Extended Properties");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_INDICATE, properties)) {
            availProperties.add("Indicate");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_NOTIFY, properties)) {
            availProperties.add("Notify");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_READ, properties)) {
            availProperties.add("Read");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, properties)) {
            availProperties.add("Signed Write");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_WRITE, properties)) {
            availProperties.add("Write");
        }
        if (isPropertySet(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, properties)) {
            availProperties.add("Write no response");
        }
        return availProperties;
    }


    private Queue<Request> initGatt(byte[] bondKey) {
        LinkedList<Request> req = new LinkedList<>();
        req.push(Request.newEnableIndicationsRequest(mReceiveDataCharacteristic));
        req.push(Request.newEnableIndicationsRequest(mResponseCharacteristic));
//        req.push(Request.newEnableNotificationsRequest(mResponseCharacteristic));
        req.push(Request.newEnableNotificationsRequest(mHeartBeatCharacteristic));
//        req.push(Request.newEnableIndicationsRequest(mHeartBeatCharacteristic));
        req.push(Request.newWriteRequest(mAuthenticationCharacteristic, bondKey));
        return req;
    }

    protected static final class Request {
        private enum Type {
            WRITE,
            READ,
            ENABLE_NOTIFICATIONS,
            ENABLE_INDICATIONS
        }

        private final Type type;
        private final BluetoothGattCharacteristic characteristic;
        private final byte[] value;

        private Request(final Type type, final BluetoothGattCharacteristic characteristic) {
            this.type = type;
            this.characteristic = characteristic;
            this.value = null;
        }

        private Request(final Type type, final BluetoothGattCharacteristic characteristic, final byte[] value) {
            this.type = type;
            this.characteristic = characteristic;
            this.value = value;
        }

        public static Request newReadRequest(final BluetoothGattCharacteristic characteristic) {
            return new Request(Type.READ, characteristic);
        }

        public static Request newWriteRequest(final BluetoothGattCharacteristic characteristic, final byte[] value) {
            return new Request(Type.WRITE, characteristic, value);
        }

        public static Request newEnableNotificationsRequest(final BluetoothGattCharacteristic characteristic) {
            return new Request(Type.ENABLE_NOTIFICATIONS, characteristic);
        }

        public static Request newEnableIndicationsRequest(final BluetoothGattCharacteristic characteristic) {
            return new Request(Type.ENABLE_INDICATIONS, characteristic);
        }
    }

    private void nextRequest() {
        Log.e(TAG, "nextRequest called");
        final Queue<Request> requests = mInitQueue;
        if (requests == null) {
            Log.e(TAG, "Requests object has not been initialized");
            return;
        }

        // Get the first request from the queue
        final Request request = requests.poll();
        Log.e(TAG, "Queue size: " + requests.size());

        // Are we done?
        if (request == null) {
            if (mInitInProgress) {
                mInitInProgress = false;
                Log.w(TAG, "Device should be ready now");
                Observable.just(G4ConnectionState.CONNECTED).subscribe(connectionStateListener);
                finalCallback = true;
            }
            return;
        }

        switch (request.type) {
            case READ: {
                Log.v(TAG, "Reading characteristic (from queued commands) " + request.characteristic.getUuid().toString());
                readCharacteristic(request.characteristic);
                break;
            }
            case WRITE: {
                if (request.characteristic == null) {
                    Log.e(TAG, "Characteristic is null");
                }

                Log.v(TAG, "Writing characteristic (from queued commands) " + request.characteristic.getUuid().toString() + " " + new String(request.value));
                final BluetoothGattCharacteristic characteristic = request.characteristic;
                characteristic.setValue(request.value);
                writeCharacteristic(characteristic);
                break;
            }
            case ENABLE_NOTIFICATIONS: {
                Log.v(TAG, "Enabling notifications on characteristic (from queued commands) " + request.characteristic.getUuid().toString());
                Log.v(TAG, "Notification status: " + enableNotifications(request.characteristic));
                break;
            }
            case ENABLE_INDICATIONS: {
                Log.v(TAG, "Enabling indications on characteristic (from queued commands) " + request.characteristic.getUuid().toString());
                Log.v(TAG, "Indication status: " + enableIndications(request.characteristic));
                break;
            }
        }
    }

    protected final boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            return false;

        Log.v(TAG, "Reading characteristic " + characteristic.getUuid());
        Log.d(TAG, "gatt.readCharacteristic(" + characteristic.getUuid() + ")");
        return gatt.readCharacteristic(characteristic);
    }

    protected final boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;

        Log.v(TAG, "Writing characteristic " + characteristic.getUuid());
        Log.d(TAG, "gatt.writeCharacteristic(" + characteristic.getUuid() + ")");
        return gatt.writeCharacteristic(characteristic);
    }

    private void setConnectionState(G4ConnectionState connectionState) {
        this.connectionState = connectionState;
        Observable.just(connectionState).subscribe(connectionStateListener);
    }


}