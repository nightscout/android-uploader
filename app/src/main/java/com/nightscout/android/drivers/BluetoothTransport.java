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

import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.DeviceTransport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.functions.Action1;

public class BluetoothTransport implements DeviceTransport {

    private final String TAG = BluetoothTransport.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mCharAuthentication;
    private BluetoothGattCharacteristic mCharSendData;
    private BluetoothGattCharacteristic mCharReceiveData;
    private BluetoothGattCharacteristic mCharCommand;
    private BluetoothGattCharacteristic mCharResponse;
    private BluetoothGattCharacteristic mCharHeartBeat;

    private String response;

    public int currentGattTask;

    public final int GATT_NOTHING = 0;
    public final int GATT_SETUP = 1;
    public final int GATT_WRITING_COMMANDS = 2;
    public final int GATT_READING_RESPONSE = 3;
    public int successfulWrites;

    public boolean shouldDisconnect = true;


    private BluetoothDevice device;


    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private Context context;


    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "CALLBACK RECIEVED Bonded");
                    authenticateConnection();

                } else if (state == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "CALLBACK RECIEVED: Not Bonded");
                } else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "CALLBACK RECIEVED: Trying to bond");
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.w(TAG, "Gatt state change status: " + status + " new state: " + newState);
            writeStatusFailures(status);
            if (status == 133) {
                Log.e(TAG, "Got the status 133 bug, GROSS!!");
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                device = mBluetoothGatt.getDevice();
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                Log.w(TAG, "Connected to GATT server.");

                Log.w(TAG, "discovering services");
                currentGattTask = GATT_SETUP;
                if (!mBluetoothGatt.discoverServices()) {
                    Log.w(TAG, "discovering failed");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                Log.w(TAG, "Disconnected from GATT server.");
            } else {
                Log.w(TAG, "Gatt callback... strange state.");
            }
            //writeStatusFailures(status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("XXX", "Services Discovered!");
                assignCharacteristics();
                authenticateConnection();
            } else {
                Log.w(TAG, "No Services Discovered!");
                writeStatusFailures(status);
            }
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("XXX", "Wrote a characteristic successfully " + characteristic.getUuid());

                if (mCharAuthentication.getUuid().equals(characteristic.getUuid())) {
                    mBluetoothGatt.readCharacteristic(mCharHeartBeat);
                }

            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    device.createBond();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing Characteristic: " + characteristic.getUuid());
                writeStatusFailures(status);
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charUuid = characteristic.getUuid();
            Log.w("XXX", "Characteristic Update Received: " + charUuid);
            if (charUuid.compareTo(mCharReceiveData.getUuid()) == 0) {
                Log.w("XXX", "mCharReceiveData Update");
                byte[] value = characteristic.getValue();
                if (value != null) {

                    Log.d("XXX", "New value in: " + Utils.bytesToHex(value));
                    response += value;
//                    Observable.just(characteristic.getValue()).subscribe(mDataResponseListener);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("XXX", "Characteristic onDescriptorWrite  UUID" + descriptor.getUuid());
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                if (mCharHeartBeat.getUuid().equals(characteristic.getUuid())) {
                    setCharacteristicIndication(mCharReceiveData);
                    setCharacteristicNotification(mCharResponse);
                }

                if (mCharReceiveData.getUuid().equals(characteristic.getUuid())) {
                    setCharacteristicIndication(mCharResponse);
                }

                if (mCharResponse.getUuid().equals(characteristic.getUuid())) {
                    Log.d("XXX", "SUCCESSFUL write");
//                    setCharacteristicNotification(mCharResponse);
//                    attemptRead();
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    //bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
                writeStatusFailures(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("XXX", "Characteristic Read " + characteristic.getUuid());
                if (mCharHeartBeat.getUuid().equals(characteristic.getUuid())) {
                    Log.w("XXX", "Characteristic Read " + characteristic.getUuid() + " " + characteristic.getValue());
                    setCharacteristicNotification(mCharHeartBeat);
                }
                mBluetoothGatt.readCharacteristic(mCharHeartBeat);
            } else {
                Log.e(TAG, "Characteristic failed to read");
                writeStatusFailures(status);
            }
        }

    };

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic) {
        setCharacteristicIndication(characteristic, true);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w("XXX", "Characteristic setting indication " + characteristic.getUuid().toString());
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        setCharacteristicNotification(characteristic, true);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w("XXX", "Characteristic setting notification " + characteristic.getUuid().toString());
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DexShareAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }


    public void assignCharacteristics() {
        mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService);
        mCharSendData = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
        mCharReceiveData = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse);
        mCharCommand = mShareService.getCharacteristic(DexShareAttributes.Command);
        mCharResponse = mShareService.getCharacteristic(DexShareAttributes.Response);
        mCharHeartBeat = mShareService.getCharacteristic(DexShareAttributes.HeartBeat);
    }

    private void writeStatusFailures(int status) {
        switch (status) {
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                Log.e(TAG, "error GATT_WRITE_NOT_PERMITTED");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                Log.e(TAG, "error GATT_INSUFFICIENT_AUTHENTICATION");
                break;
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                Log.e(TAG, "error GATT_REQUEST_NOT_SUPPORTED");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                Log.e(TAG, "error GATT_INSUFFICIENT_ENCRYPTION");
                break;
            case BluetoothGatt.GATT_INVALID_OFFSET:
                Log.e(TAG, "error GATT_INVALID_OFFSET");
                break;
            case BluetoothGatt.GATT_FAILURE:
                Log.e(TAG, "error GATT_FAILURE");
                break;
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                Log.e(TAG, "error GATT_INVALID_ATTRIBUTE_LENGTH");
                break;
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                Log.e(TAG, "error GATT_READ_NOT_PERMITTED");
                break;
            case BluetoothGatt.GATT_SUCCESS:
                Log.d(TAG, "success GATT_SUCCESS");
                break;
            default:
                Log.e(TAG, "error no idea!");
                break;
        }
    }

    public BluetoothTransport(Context context) {
        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.context = context;
        context.registerReceiver(mPairReceiver, bondintent);
    }

    @Override
    public void open() throws IOException {
        AndroidPreferences prefs = new AndroidPreferences(context);
        String address = prefs.getBtAddress();
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
                if (bluetoothDevice.getAddress().equals(address)) {
                    Log.w(TAG, "Device found, already bonded, going to connect");
                    bluetoothDevice.connectGatt(context, false, mGattCallback);
                }
            }
        }
    }


    @Override
    public void close() throws IOException {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
        Log.w(TAG, "bt Disconnected");
    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        return 0;
    }

    @Override
    public byte[] read(int size, int timeoutMillis) throws IOException {
        byte[] resp;
        Action1<byte[]> reader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {

            }
        };

        return new byte[0];
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {
//        List<byte[]> packets = chunkPacket(src);
//        Log.d(TAG, "Writing: " + Utils.bytesToHex(packets.get(0)));
        Log.d(TAG, "Writing: " + Utils.bytesToHex(src));
        mCharSendData.setValue(src);
        mBluetoothGatt.writeCharacteristic(mCharSendData);
        return 0;
    }

    // TODO maybe look into uppping the MTU
    private List<byte[]> chunkPacket(byte[] packet) {
        List<byte[]> packetChunkList = new ArrayList<>();
        int chunkCount = (int) Math.ceil(packet.length / 18);
        int packetSize = 20;
        for (int i = 0; i < chunkCount; i++) {
            if (i == chunkCount - 1) {
                packetSize = ((packet.length + 2) % 18);
            }
            int offset = i * 18;
            Log.d("ShareTest", "This packet size: " + packetSize);
            byte[] b = new byte[packetSize];
            b[0] = (byte) (i + 1);
            b[1] = (byte) (chunkCount);
            System.arraycopy(packet, offset + 2 - 2, b, 2, packetSize - 2);
            packetChunkList.add(b);
        }
        return packetChunkList;
    }

    @Override
    public boolean isConnected(int vendorId, int productId, int deviceClass, int subClass, int protocol) {
        return true;
    }

    //    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void authenticateConnection() {
        Log.w(TAG, "Trying to auth");
//        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
        String receiverSn = "SM50556654";
//        String receiverSn = "SM00000000";
        byte[] bondkey;

        bondkey = (receiverSn + "000000").getBytes(StandardCharsets.US_ASCII);

        if (mBluetoothGatt != null) {

            if (mShareService != null) {
                mCharAuthentication = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode);
                if (mCharAuthentication != null) {
                    Log.w(TAG, "Auth Characteristic found: " + mCharAuthentication.toString());
                    if (mCharAuthentication.setValue(bondkey)) {
                        mBluetoothGatt.writeCharacteristic(mCharAuthentication);
                    }
                } else {
                    Log.w(TAG, "Authentication Characteristic IS NULL");
                }
            } else {
                Log.w(TAG, "CRADLE SERVICE IS NULL");
            }
        }
    }

//    public void attemptConnection() {
//        if(mBluetoothManager==null) {
//            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
//        }
//
//
//        if (mBluetoothManager != null) {
//            Log.w(TAG, "Connection state: " + mConnectionState);
//            if (mConnectionState == BluetoothProfile.STATE_DISCONNECTED ) {
//                AndroidPreferences prefs = new AndroidPreferences(context);
//                String btAddress = prefs.getBtAddress();
//                if (!btAddress.equals("")) {
//                    mBluetoothAdapter = mBluetoothManager.getAdapter();
//                    if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getRemoteDevice(btAddress) != null) {
//                        connect(btAddress);
//                    } else {
//                        Log.w(TAG, "Bluetooth is disabled or BT device cant be found");
////                        setRetryTimer();
//                    }
//                } else {
//                    Log.w(TAG, "No bluetooth device to try and connect to");
////                    setRetryTimer();
//                }
//            } else if (mConnectionState == BluetoothProfile.STATE_CONNECTED) {
//                Log.w(TAG, "Looks like we are already connected, going to read!");
//                attemptRead();
////            } else {
////                setRetryTimer();
//            }
////        } else {
////            setRetryTimer();
//        }
//    }

//    public boolean connect(final String address) {
//        Log.w(TAG, "going to connect to device at address" + address);
//
//        if (mBluetoothGatt != null) {
//            Log.w(TAG, "BGatt isnt null, Closing.");
//            mBluetoothGatt.disconnect();
//            mBluetoothGatt.close();
//            mBluetoothGatt = null;
//            //mBluetoothGatt.connect();
//        }
//        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
//            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
//                Log.w(TAG, "Device found, already bonded, going to connect");
//                if(mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress()) != null) {
//                    device = bluetoothDevice;
//                    ///device.setPin("000000".getBytes());
//                    mBluetoothGatt = device.connectGatt(context.getApplicationContext(), false, mGattCallback);
////                   refreshDeviceCache(mBluetoothGatt);
//                    return true;
//                }
//            }
//        }
//        device = mBluetoothAdapter.getRemoteDevice(address);
//        if (device == null) {
//            Log.w(TAG, "Device not found.  Unable to connect.");
////            setRetryTimer();
//            return false;
//        }
//        //device.setPin("000000".getBytes());
//        Log.w(TAG, "Trying to create a new connection.");
//        mBluetoothGatt = device.connectGatt(context.getApplicationContext(), true, mGattCallback);
//        mConnectionState = BluetoothProfile.STATE_CONNECTING;
//        return true;
//    }
}
