package com.nightscout.core.dexcom;

public class Constants {
    public final static int MAX_COMMAND = 59;
    public final static int MAX_POSSIBLE_COMMAND = 255;
    public final static int EGV_VALUE_MASK = 1023;
    public final static int EGV_DISPLAY_ONLY_MASK = 32768;
    public final static int EGV_TREND_ARROW_MASK = 15;
    public final static int EGV_NOISE_MASK = 112;
    public final static float MG_DL_TO_MMOL_L = 0.05556f;
    public final static int CRC_LEN = 2;
    public static final float MMOL_L_TO_MG_DL = 18.0182f;
}
