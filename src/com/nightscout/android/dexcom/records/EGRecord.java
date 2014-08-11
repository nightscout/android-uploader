package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Constants;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EGRecord implements Serializable {

    private int displayTime;
    private int bGValue;
    private String trend;

    public EGRecord(byte[] packet) {
        // uint, uint, ushort, byte, ushort
        // (system_time, display_time, glucose, trend_arrow, crc)
        // TODO: convert time using epoch
        displayTime = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        int eGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        bGValue = eGValue & Constants.EGV_VALUE_MASK;
        int trendValue = ByteBuffer.wrap(packet).get(10) & Constants.EGV_TREND_ARROW_MASK;
        trend = Constants.TREND_ARROW_VALUES.values()[trendValue].friendlyTrendName();
    }

    public int getDisplayTime() {
        return displayTime;
    }

    public int getBGValue() {
        return bGValue;
    }

    public String getTrend() {
        return trend;
    }
}
