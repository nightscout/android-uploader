package com.nightscout.android.dexcom.USB;

import android.util.Log;
import com.nightscout.android.dexcom.DexcomG4Activity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class USBPower {

    private static final String TAG = "DexcomUSBPower";

    private static final String SET_POWER_ON_COMMAND = "echo 'on' > \"/sys/bus/usb/devices/1-1/power/level\"";
    private static final String SET_POWER_SUSPEND_COMMAND_A = "echo \"0\" > \"/sys/bus/usb/devices/1-1/power/autosuspend\"";
    private static final String SET_POWER_SUSPEND_COMMAND_B = "echo \"suspend\" > \"/sys/bus/usb/devices/1-1/power/level\"";
    private static final String ENABLE_COMMAND = "echo \"1\" > \"/sys/bus/usb/devices/1-1/bConfigurationValue\"";
    private static final String ENABLE_FILE = "/sys/bus/usb/devices/1-1/bConfigurationValue";

    private static String readFile(String filename) {
      String retVal = null;
      try {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        retVal = br.readLine();
      } catch (Exception e) {
        Log.e(TAG, e.getLocalizedMessage() + "");
      }
      return retVal;
    }

    public static void enable() {
        String status = readFile(ENABLE_FILE);
        if (status == null) {
            try {
                runCommand(ENABLE_COMMAND);
                Log.i(TAG, "Enabled USB device");
            } catch (Exception e) {
                Log.e(TAG, "USB device already enabled");
            }
        }
    }

    public static void PowerOff() {
        try {
            runCommand(SET_POWER_SUSPEND_COMMAND_A);
            runCommand(SET_POWER_SUSPEND_COMMAND_B);
            Log.i(TAG, "PowerOff USB complete");
        } catch (Exception e) {
            Log.e(TAG, "Unable to PowerOff USB");
        }
    }

    public static void PowerOn(){
        try {
            enable();
            runCommand(SET_POWER_ON_COMMAND);
            Log.i(TAG, "PowerOn USB complete");
        } catch (Exception e) {
            Log.e(TAG, "Unable to PowerOn USB");
        }
    }

    private static void runCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        os.writeBytes(command + "\n");
        os.flush();
        os.writeBytes("exit \n");
        os.flush();
        os.close();
        process.waitFor();
    }
}
