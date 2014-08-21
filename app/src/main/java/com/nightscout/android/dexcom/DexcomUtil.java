package com.nightscout.android.dexcom;

public class DexcomUtil {
    public static Byte[] toByteObjectArray(byte[] input) {
        Byte[] output = new Byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Byte.valueOf(input[i]);
        }
        return output;
    }
}
