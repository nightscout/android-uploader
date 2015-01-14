package com.nightscout.android.dexcom.USB;

import android.util.Log;

import java.io.DataOutputStream;

public class USBPower {

    private static final String TAG = USBPower.class.getSimpleName();

    private static final String SET_POWER_ON_COMMAND = "echo 'on' > \"/sys/bus/usb/devices/1-1/power/control\"";
    private static final String SET_POWER_SUSPEND_COMMAND_A = "echo \"0\" > \"/sys/bus/usb/devices/1-1/power/autosuspend_delay_ms\"";
    private static final String SET_POWER_SUSPEND_COMMAND_B = "echo \"auto\" > \"/sys/bus/usb/devices/1-1/power/control\"";
    public static final int POWER_ON_DELAY = 5000;

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
            runCommand(SET_POWER_ON_COMMAND);
            Log.i(TAG, "PowerOn USB complete");
            Thread.sleep(POWER_ON_DELAY);
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
