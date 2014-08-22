package com.nightscout.android.dexcom.records;

import com.nightscout.android.dexcom.Constants;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class EGRecord extends GenericTimestampRecord implements Serializable {

    private int bGValue;
    private String trend;

    public EGRecord(byte[] packet) {
        // system_time (UInt), display_time (UInt), glucose (UShort), trend_arrow (Byte), crc (UShort))
        super(packet);
        int eGValue = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getShort(8);
        bGValue = eGValue & Constants.EGV_VALUE_MASK;
        int trendValue = ByteBuffer.wrap(packet).get(10) & Constants.EGV_TREND_ARROW_MASK;
        trend = Constants.TREND_ARROW_VALUES.values()[trendValue].friendlyTrendName();
    }

    public int getBGValue() {
        return bGValue;
    }

    public String getTrend() {
        return trend;
    }
}
