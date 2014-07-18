package com.nightscout.android.dexcom.USB;

import android.util.Log;
import com.nightscout.android.dexcom.DexcomG4Activity;

import java.io.DataOutputStream;
import java.io.IOException;

public class USBPower {
	
	private static final String TAG = DexcomG4Activity.class.getSimpleName();

	private static final String SET_POWER_ON_COMMAND = "echo 'on' > \"/sys/bus/usb/devices/1-1/power/level\"";
	private static final String SET_POWER_SUSPEND_COMMAND_A = "echo \"0\" > \"/sys/bus/usb/devices/1-1/power/autosuspend\"";
	private static final String SET_POWER_SUSPEND_COMMAND_B = "echo \"auto\" > \"/sys/bus/usb/devices/1-1/power/level\"";
	
	
	public static void PowerOff(){
		runCommand(SET_POWER_SUSPEND_COMMAND_A);
		runCommand(SET_POWER_SUSPEND_COMMAND_B);
		
	}
	
	public static void PowerOn(){
		runCommand(SET_POWER_ON_COMMAND);
	}

	private static void runCommand(String command) {
		try {

			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.flush();
			os.writeBytes("exit \n");
			os.flush();
			os.close();
			process.waitFor();

		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}
