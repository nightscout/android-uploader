package com.nightscout.android.drivers.USB;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class CareLinkUsb extends CommonUsbSerialDriver {
    private static final int MAX_PACKAGE_SIZE = 64;

    private static final String TAG = "CareLinkUsb";
    //    private static CareLinkUsb instance;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mInterface;
    private UsbEndpoint epIN, epOUT;
    private UsbRequest currentRequest;

    public CareLinkUsb(UsbDevice device, UsbDeviceConnection connection, UsbManager manager) {
        super(device, connection, manager);
    }

//    private void initCareLink(){
//        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
//
//        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
//
//        Log.d(TAG, "Enumerating connected devices...");
//
//        // Getting the CareLink UsbDevice object
//        for (UsbDevice usbDevice : deviceList.values()) {
//            mUsbDevice = usbDevice;
//            if (mUsbDevice.getVendorId() == VENDOR_ID && mUsbDevice.getProductId() == PRODUCT_ID) {
//                break;
//            }
//        }
//
//        Log.d(TAG, "Device found\nVendorId: " + mUsbDevice.getVendorId() + "\nProductId: " + mUsbDevice.getProductId());
//    }

    /**
     * Opens a connection to the connected CareLink stick
     *
     * @throws IOException
     */
    public void open() throws IOException {
        if (mUsbDeviceConnection != null) {
            Log.d(TAG, "There is already a connection open to the device!");
            return;
        }

        // Assigning interface
        mInterface = mDevice.getInterface(0);

        // Assigning endpoint in and out
        epOUT = mInterface.getEndpoint(0);
        epIN = mInterface.getEndpoint(1);


        // Open connection
        Log.d(TAG, "Opening connection...");
        mUsbDeviceConnection = mManager.openDevice(mDevice);
//        Log.d(TAG, "Opened connection to\nVendorId: " + mDevice.getVendorId() + "\nProductId: " + mDevice.getProductId());

        if (mUsbDeviceConnection == null) {
            throw new IOException("open: no connection available");
        }
    }

    /**
     * Closes the current connection to the CareLink stick
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (mUsbDeviceConnection == null) {
            throw new IOException("close: no connection available");
        }
        mUsbDeviceConnection.releaseInterface(mInterface);
        mUsbDeviceConnection.close();
    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        return 0;
    }

    @Override
    public byte[] read(int size, int timeoutMillis) throws IOException {
        Log.d(TAG, "Read size: " + size);
        if (mUsbDeviceConnection == null) {
            throw new IOException("read: no connection available");
        }

        // FIXME what is this?
        if (currentRequest == null) {
            throw new IOException("there is nothing to read");
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);

        // Receive data from device
        if (currentRequest.equals(mUsbDeviceConnection.requestWait())) {
            UsbRequest inRequest = new UsbRequest();
            inRequest.initialize(mUsbDeviceConnection, epIN);
            if (inRequest.queue(buffer, size)) {
                mUsbDeviceConnection.requestWait();
                return buffer.array();
            }
        }
        return null;
    }

    @Override
    public int write(byte[] src, int timeoutMillis) throws IOException {
        if (mUsbDeviceConnection == null) {
            throw new IOException("write: no connection available");
        }

        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKAGE_SIZE);

        currentRequest = new UsbRequest();
        currentRequest.initialize(mUsbDeviceConnection, epOUT);

        buffer.put(src);
        currentRequest.queue(buffer, MAX_PACKAGE_SIZE);
        return 0;
    }

    /**
     * Gets the response from the connected device.
     *
     * @return Returns the response in a byte[].
     */
    public byte[] read() throws IOException {
        if (mUsbDeviceConnection == null) {
            throw new IOException("read: no connection available");
        }

        // FIXME what is this?
        if (currentRequest == null) {
            throw new IOException("there is nothing to read");
        }

        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKAGE_SIZE);

        // Receive data from device
        if (currentRequest.equals(mUsbDeviceConnection.requestWait())) {
            UsbRequest inRequest = new UsbRequest();
            inRequest.initialize(mUsbDeviceConnection, epIN);
            if (inRequest.queue(buffer, MAX_PACKAGE_SIZE)) {
                mUsbDeviceConnection.requestWait();
                return buffer.array();
            }
        }
        return null;
    }

    /**
     * Write a command to the connected device.
     *
     * @param command Byte[] containing the opcode for the command.
     * @return Returns the response in a byte[].
     */
    public void write(byte[] command) throws IOException {
        if (mUsbDeviceConnection == null) {
            throw new IOException("write: no connection available");
        }

        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKAGE_SIZE);

        currentRequest = new UsbRequest();
        currentRequest.initialize(mUsbDeviceConnection, epOUT);

        buffer.put(command);
        currentRequest.queue(buffer, MAX_PACKAGE_SIZE);
    }

    /**
     * Wrapper for CareLinkUsb.write() and CareLinkUsb.read()
     *
     * @param command Byte[] containing the opcode for the command.
     * @return Returns a reference to the UsbRequest put in the output queue.
     */
    public byte[] sendCommand(byte[] command) throws IOException {
        write(command);
        return read();
    }

    public UsbDevice getUsbDevice() {
        return mDevice;
    }


    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {

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
        return false;
    }

    @Override
    public void setDTR(boolean value) throws IOException {

    }

    @Override
    public boolean getRI() throws IOException {
        return false;
    }

    @Override
    public boolean getRTS() throws IOException {
        return false;
    }

    @Override
    public void setRTS(boolean value) throws IOException {

    }

    public static Map<Integer, Integer[]> getSupportedDevices() {
        final Map<Integer, Integer[]> supportedDevices = new LinkedHashMap<>();
        supportedDevices.put(UsbId.VENDOR_MEDTRONIC,
                new Integer[]{
                        UsbId.MEDTRONIC_CARELINK_PRODUCT
                });
        return supportedDevices;
    }

}