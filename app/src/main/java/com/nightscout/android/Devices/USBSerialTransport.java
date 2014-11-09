package com.nightscout.android.devices;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.nightscout.android.dexcom.USB.USBPower;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.USB.UsbSerialProber;

import java.io.IOException;

public class USBSerialTransport extends DeviceTransportAbstract {
    private static final String TAG = USBSerialTransport.class.getSimpleName();
    private UsbSerialDriver mSerialDevice;
    Context appContext;
    private boolean usePowerManagement=false;


    public USBSerialTransport(Context c){
        appContext=c;
    }

    @Override
    public boolean open() throws IOException {
        if (usePowerManagement) {
            USBPower.PowerOn();
        }
        if (! isOpen()){
            Log.v(TAG, "Attempting to connect");
            UsbManager mUsbManager;
            mUsbManager=(UsbManager) appContext.getSystemService(Context.USB_SERVICE);
            mSerialDevice = UsbSerialProber.acquire(mUsbManager);
            if (mSerialDevice != null) {
                try {
                    mSerialDevice.open();
                    Log.d(TAG, "Successfully connected");
                    isopen = true;
                } catch (IOException e) {
                    Log.e(TAG, "Unable to establish a serial connection to Dexcom G4",e);
                    isopen = false;
                    throw new IOException("Unable to establish a serial connection to Dexcom G4");
                }
            }else{
                Log.e(TAG,"Unable to acquire USB Manager");
                throw new IOException("Unable to acquire USB manager");
            }
        }else{
            Log.w(TAG, "Already connected");
        }

        return false;
    }

    @Override
    public void close() {
        if (isOpen() && mSerialDevice!=null) {
            Log.v(TAG, "Attempting to disconnect");
            try {
                mSerialDevice.close();
                Log.d(TAG, "Successfully disconnected");
                isopen = false;
                if (usePowerManagement) {
                    Log.v(TAG,"chargeReceiver: "+usePowerManagement);
                    Log.d(TAG,"Disabling USB power");
                    USBPower.PowerOff();
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to close serial connection to Dexcom G4");
            }
        } else {
            Log.w(TAG,"Already disconnected");
        }
    }

    @Override
    public int read(byte[] responseBuffer,int timeoutMillis) throws IOException {
        int bytesRead;
        bytesRead=mSerialDevice.read(responseBuffer,timeoutMillis);
        totalBytesRead+=bytesRead;
        return bytesRead;
    }

    public void setUsePowerManagement(boolean usePowerManagement) {
        this.usePowerManagement = usePowerManagement;
    }

    @Override
    public int write(byte [] packet, int writeTimeout) throws IOException {
        int bytesWritten;
        bytesWritten=mSerialDevice.write(packet, writeTimeout);
        totalBytesWritten+=bytesWritten;
        return bytesWritten;
    }
}