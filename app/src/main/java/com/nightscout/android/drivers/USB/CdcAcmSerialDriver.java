package com.nightscout.android.drivers.USB;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.nightscout.core.drivers.G4ConnectionState;

import java.io.IOException;
import java.util.Arrays;

import rx.Observable;

//import com.hoho.android.usbserial.driver.UsbId;

/**
 * USB CDC/ACM serial driver implementation.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @see <a
 * href="http://www.usb.org/developers/devclass_docs/usbcdc11.pdf">Universal
 * Serial Bus Class Definitions for Communication Devices, v1.1</a>
 */
public class CdcAcmSerialDriver extends CommonUsbSerialDriver {

    private final String TAG = CdcAcmSerialDriver.class.getSimpleName();

    private UsbInterface mControlInterface;
    private UsbInterface mDataInterface;

    private UsbEndpoint mControlEndpoint;
    private UsbEndpoint mReadEndpoint;
    private UsbEndpoint mWriteEndpoint;

    private boolean mRts = false;
    private boolean mDtr = false;

    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

    private static final int SET_LINE_CODING = 0x20;  // USB CDC 1.1 section 6.2
    private static final int GET_LINE_CODING = 0x21;
    private static final int SET_CONTROL_LINE_STATE = 0x22;
    private static final int SEND_BREAK = 0x23;
    private G4ConnectionState connectionState;
    private static final String SET_POWER_ON_COMMAND = "echo 'on' > \"/sys/bus/usb/devices/1-1/power/level\"";

    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Log.d(TAG, "Stopping syncing on USB attached...");
                    setConnectionState(G4ConnectionState.CLOSING);
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    setConnectionState(G4ConnectionState.CONNECTING);
                    Log.d(TAG, "Starting syncing on USB attached...");
                    break;
            }
        }
    };


//    protected Context context;

    public CdcAcmSerialDriver(UsbDevice device, UsbDeviceConnection connection, UsbManager manager, Context context) {
        super(device, connection, manager);
        IntentFilter deviceStatusFilter = new IntentFilter();
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mDeviceStatusReceiver, deviceStatusFilter);
    }

    @Override
    public void open() throws IOException {
        setConnectionState(G4ConnectionState.CONNECTING);
        if (mPowerManagementEnabled) {
            USBPower.powerOn();
        }
        Log.d(TAG, "claiming interfaces, count=" + mDevice.getInterfaceCount());

        mControlInterface = mDevice.getInterface(0);
        Log.d(TAG, "Control iface=" + mControlInterface);
        // class should be USB_CLASS_COMM

        if (!mConnection.claimInterface(mControlInterface, true)) {
            Observable.just(G4ConnectionState.CLOSED).subscribe(connectionStateListener);
            throw new IOException("Could not claim control interface.");
        }
        mControlEndpoint = mControlInterface.getEndpoint(0);
        Log.d(TAG, "Control endpoint direction: " + mControlEndpoint.getDirection());

        mDataInterface = mDevice.getInterface(1);
        Log.d(TAG, "data iface=" + mDataInterface);
        // class should be USB_CLASS_CDC_DATA

        if (!mConnection.claimInterface(mDataInterface, true)) {
            Observable.just(G4ConnectionState.CLOSED).subscribe(connectionStateListener);
            throw new IOException("Could not claim data interface.");
        }
        mReadEndpoint = mDataInterface.getEndpoint(1);
        Log.d(TAG, "Read endpoint direction: " + mReadEndpoint.getDirection());
        mWriteEndpoint = mDataInterface.getEndpoint(0);
        Log.d(TAG, "Write endpoint direction: " + mWriteEndpoint.getDirection());
        setConnectionState(G4ConnectionState.CONNECTED);
    }

    private int sendAcmControlMessage(int request, int value, byte[] buf) {
        return mConnection.controlTransfer(
                USB_RT_ACM, request, value, 0, buf, buf != null ? buf.length : 0, 5000);
    }

    @Override
    public void close() throws IOException {
        setConnectionState(G4ConnectionState.CLOSING);
        mConnection.close();
        if (mPowerManagementEnabled) {
            USBPower.powerOff();
        }
        setConnectionState(G4ConnectionState.CLOSED);
    }

    public byte[] read(int size, int timeoutMillis) throws IOException {
//        timeoutMillis = 2000;
//        size = 2122;
        if (size < 2122) {
            Log.i(TAG, "Adjusting requested size of " + size + " to 2122");
            size = 2122;
        }
        byte[] data = new byte[size];

        int readSize = read(data, timeoutMillis);
        return Arrays.copyOfRange(data, 0, readSize);
    }

    private void setConnectionState(G4ConnectionState connectionState) {
        this.connectionState = connectionState;
        Observable.just(connectionState).subscribe(connectionStateListener);
    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        if (connectionState != G4ConnectionState.CONNECTED && connectionState != G4ConnectionState.IDLE) {
            throw new IOException("Attempted to read while not connected. Current state: " + connectionState.name());
        }
        setConnectionState(G4ConnectionState.READING);
        Log.w(TAG, "Dest: " + dest.length + " Buffer: " + mReadBuffer.length);

        final int numBytesRead;
        synchronized (mReadBufferLock) {
            int readAmt = Math.min(dest.length, mReadBuffer.length);
            numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                    timeoutMillis);
            if (numBytesRead < 0) {
                Log.d(TAG, "Read timeout occurred.");
                throw new IOException("Read timeout");
                // This sucks: we get -1 on timeout, not 0 as preferred.
                // We *should* use UsbRequest, except it has a bug/api oversight
                // where there is no way to determine the number of bytes read
                // in response :\ -- http://b.android.com/28023
//                return 0;
            }
            System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
        }
        setConnectionState(G4ConnectionState.IDLE);
        return numBytesRead;
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {
        if (connectionState != G4ConnectionState.CONNECTED && connectionState != G4ConnectionState.IDLE) {
            throw new IOException("Attempted to write while not connected. Current state: " + connectionState.name());
        }
        setConnectionState(G4ConnectionState.WRITING);
        int offset = 0;

        while (offset < src.length) {
            final int writeLength;
            final int amtWritten;

            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                if (offset == 0) {
                    writeBuffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                    writeBuffer = mWriteBuffer;
                }

                amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                        timeoutMillis);
            }
            if (amtWritten <= 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + offset + " length=" + src.length);
            }

            Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
            offset += amtWritten;
        }
        setConnectionState(G4ConnectionState.IDLE);

        return offset;
    }

    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {
        byte stopBitsByte;
        switch (stopBits) {
            case STOPBITS_1:
                stopBitsByte = 0;
                break;
            case STOPBITS_1_5:
                stopBitsByte = 1;
                break;
            case STOPBITS_2:
                stopBitsByte = 2;
                break;
            default:
                throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
        }

        byte parityBitesByte;
        switch (parity) {
            case PARITY_NONE:
                parityBitesByte = 0;
                break;
            case PARITY_ODD:
                parityBitesByte = 1;
                break;
            case PARITY_EVEN:
                parityBitesByte = 2;
                break;
            case PARITY_MARK:
                parityBitesByte = 3;
                break;
            case PARITY_SPACE:
                parityBitesByte = 4;
                break;
            default:
                throw new IllegalArgumentException("Bad value for parity: " + parity);
        }

        byte[] msg = {
                (byte) (baudRate & 0xff),
                (byte) ((baudRate >> 8) & 0xff),
                (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff),
                stopBitsByte,
                parityBitesByte,
                (byte) dataBits};
        sendAcmControlMessage(SET_LINE_CODING, 0, msg);
    }

    @Override
    public boolean getCD() throws IOException {
        return false;
    }

    @Override
    public boolean getCTS() throws IOException {
        return false;
    }

    @Override
    public boolean getDSR() throws IOException {
        return false;
    }

    @Override
    public boolean getDTR() throws IOException {
        return mDtr;
    }

    @Override
    public void setDTR(boolean value) throws IOException {
        mDtr = value;
        setDtrRts();
    }

    @Override
    public boolean getRI() throws IOException {
        return false;
    }

    @Override
    public boolean getRTS() throws IOException {
        return mRts;
    }

    @Override
    public void setRTS(boolean value) throws IOException {
        mRts = value;
        setDtrRts();
    }

    private void setDtrRts() {
        int value = (mRts ? 0x2 : 0) | (mDtr ? 0x1 : 0);
        sendAcmControlMessage(SET_CONTROL_LINE_STATE, value, null);
    }
}
